#include "includes.h"
#include "socketUtils.h"
#include "connection.h"

#define MAX_CONNECTIONS 2048
#define log_print printf

static struct connection connections[MAX_CONNECTIONS];
static ssize_t connectionsCount = 0;
int listenSocket, dnsSocket;
int num_fd;

static char *failedAuth = "\x05\xFF";
static char *successAuth = "\x05\x00";

int sendDNS(int i); //send async dns request

char *parseDNS(struct dns_packet *ans); //extract ip from dns packet

void form_poll_fds(struct pollfd *fds);

void dropConnection(int id, char *reason, int server); //drop connection and clean up

char *errorReply(char code); //generate string with SOCKS error

char *form_response(int i); //generate correct positive response

static void exit_handler(void) {
    close(listenSocket);
    for (int i = 0; i < MAX_CONNECTIONS; i++) {
        close(connections[i].clientSocket);
    }
}

int main(int argc, char **argv) {
    fd_set readfs, writefs;
    int res;
    if (argc < 1) {
        fprintf(stderr, "Usage: %s [listen_port]\n", argv[0]);
        return 0;
    }

    char *end;
    uint16_t localPort = (uint16_t) strtol(argv[1], &end, 10);

    if (localPort <= 0) {
        fprintf(stderr, "Usage: %s [listen_port]\n", argv[0]);
        return 0;
    }
    listenSocket = openListenSocket(localPort);
    atexit(exit_handler);

    log_print("Proxy started at port %d\n", localPort);
    while (1) {
        struct pollfd fds[2 * MAX_CONNECTIONS + 2];
        form_poll_fds(fds);
        int polled = poll(fds, connectionsCount * 2 + 2, 5 * 1000);

        if (polled < 0) {
            fprintf(stderr, "Poll error\n");
        } else if (polled == 0) {
            continue;
        }
        char buf[BUFFER_SIZE];
        for (int i = 0; i < connectionsCount; ++i) {
            switch (connections[i].status) {
                case CS_INCOMING:
                    if (fds[i * 2].revents & POLLHUP) {
                        dropConnection(i, "closed by client", 0);
                    } else if (fds[i * 2].revents & POLLIN) {
                        log_print("Connection %d, start handshake\n", connections[i].id);
                        ssize_t readCount = read(connections[i].clientSocket, buf, BUFFER_SIZE);
                        if (readCount == 0) {
                            dropConnection(i, "closed by client", 0);
                            break;
                        }
                        connections[i].currBufClient += readCount;
                        char *dest = connections[i].clientBuf + connections[i].currBufClient - readCount;
                        memcpy(dest, buf, (size_t) readCount);
                        if (connections[i].currBufClient >= 2) {
                            if (connections[i].clientBuf[0] != 5) {
                                dropConnection(i, "wrong version", 0);
                                break;
                            }
                            ssize_t numMethods = connections[i].clientBuf[1];
                            if (connections[i].currBufClient >= 2 + numMethods) {
                                connections[i].status = CS_AUTH_FAILURE;
                                for (int j = 0; j < numMethods; j++) {
                                    if (connections[i].clientBuf[j + 2] == 0) {
                                        connections[i].status = CS_AUTH_SUCCESS;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    break;
                case CS_AUTH_FAILURE:
                    if (fds[i * 2].revents & POLLOUT) {
                        write(connections[i].clientSocket, failedAuth, 2);
                        dropConnection(i, "Auth failed", 0);
                    }
                    break;
                case CS_AUTH_SUCCESS:
                    if (fds[i * 2].revents & POLLOUT) {
                        log_print("Connection %d, auth success\n", connections[i].id);
                        write(connections[i].clientSocket, successAuth, 2);
                        connections[i].status = CS_SOCK_REQUEST;
                        connections[i].currBufClient = 0;
                    }
                    break;
                case CS_SOCK_REQUEST:
                    if (fds[i * 2].revents & (POLLIN | POLLOUT)) {
                        log_print("Connection %d, start reading request\n", connections[i].id);
                        ssize_t readCount = read(connections[i].clientSocket, buf, BUFFER_SIZE);
                        if (readCount == 0) {
                            dropConnection(i, "closed by client", 0);
                            break;
                        }
                        connections[i].currBufClient += readCount;
                        char *dest = connections[i].clientBuf + connections[i].currBufClient - readCount;
                        memcpy(dest, buf, (size_t) readCount);
                        if (connections[i].currBufClient >= 2) {
                            if (connections[i].clientBuf[0] != 5 || connections[i].clientBuf[1] != 1) {
                                char *error = errorReply(7);
                                write(connections[i].clientSocket, error, 10);
                                free(error);
                                dropConnection(i, "Non supported method", 0);
                            }
                        }
                        if (connections[i].currBufClient >= 5) {
                            log_print("Connection %d, command %d, address type %d\n", connections[i].id,
                                      connections[i].clientBuf[1], connections[i].clientBuf[3]);
                            if (connections[i].clientBuf[3] == 1) {
                                if (connections[i].currBufClient >= 10) {
                                    log_print("Connection %d, request with direct address parsed\n", connections[i].id);
                                    char address[4];
                                    memcpy(address, connections[i].clientBuf + 4, 4);
                                    char port[2];
                                    memcpy(port, connections[i].clientBuf + 8, 2);
                                    connections[i].serverSocket = openServerSocket(address, port);
                                    char *response = form_response(i);
                                    write(connections[i].clientSocket, response, 10);
                                    free(response);
                                    if (connections[i].serverSocket == -1) {
                                        char *error = errorReply(3);
                                        write(connections[i].clientSocket, error, 10);
                                        free(error);
                                        dropConnection(i, "Cannot connect to remote server", 0);
                                        break;
                                    }
                                    connections[i].currBufServer = connections[i].currBufClient = 0;
                                    connections[i].status = CS_FORWARDING;
                                }
                            } else if (connections[i].clientBuf[3] == 3) {
                                if (connections[i].currBufClient >= ((size_t) connections[i].clientBuf[4]) + 2) {
                                    log_print("Connection %d, dns resolve started\n", connections[i].id);
                                    connections[i].status = CS_RESOLVING;
                                    if (sendDNS(i)) {
                                        char *error = errorReply(4);
                                        write(connections[i].clientSocket, error, 10);
                                        free(error);
                                        dropConnection(i, "Resolver error", 0);
                                        break;
                                    }
                                }
                            } else {
                                char *error = errorReply(8);
                                write(connections[i].clientSocket, error, 10);
                                free(error);
                                dropConnection(i, "Wrong address type", 0);
                            }
                        }
                        break;
                    }

                case CS_RESOLVING:
                    if (fds[2 * i].revents & POLLOUT) {
                        int error = dns_res_check(connections[i].R);
                        if (error == 0) {
                            struct dns_packet *ans = dns_res_fetch(connections[i].R, &error);
                            char *ip = parseDNS(ans);
                            connections[i].serverSocket = openServerSocket(ip, connections[i].serverBuf);
                            free(ip);
                            free(ans);
                            dns_res_close(connections[i].R);
                            char *response = form_response(i);
                            write(connections[i].clientSocket, response, 10);
                            free(response);
                            if (connections[i].serverSocket == -1) {
                                char *error = errorReply(3);
                                write(connections[i].clientSocket, error, 10);
                                free(error);
                                dropConnection(i, "Cannot connect to remote server", 0);
                                break;
                            }
                            connections[i].currBufServer = connections[i].currBufClient = 0;
                            log_print("Connection %d, ended resolve\n", connections[i].id);
                            connections[i].status = CS_FORWARDING;
                            break;
                        }
                        if (error != EAGAIN) {
                            char *error = errorReply(4);
                            write(connections[i].clientSocket, error, 10);
                            free(error);
                            dns_res_close(connections[i].R);
                            dropConnection(i, "Resolve error", 0);
                            break;
                        }
                        if (dns_res_elapsed(connections[i].R) > 30) {
                            char *error = errorReply(4);
                            write(connections[i].clientSocket, error, 10);
                            free(error);
                            dns_res_close(connections[i].R);
                            dropConnection(i, "Resolve timeout", 0);
                            break;
                        }
                    }

                case CS_FORWARDING:
                    if (connections[i].currBufClient == 0 && fds[2 * i].revents & POLLIN) {
                        connections[i].currBufClient = read(connections[i].clientSocket, connections[i].clientBuf,
                                                            BUFFER_SIZE);
                        if (connections[i].currBufClient == 0) {
                            connections[i].currBufClient = -1;
                        }
                    }
                    if (connections[i].currBufServer == 0 && fds[2 * i + 1].revents & POLLIN) {
                        connections[i].currBufServer = read(connections[i].serverSocket, connections[i].serverBuf,
                                                            BUFFER_SIZE);
                        if (connections[i].currBufServer == 0) {
                            connections[i].currBufServer = -1;
                        }
                    }
                    if (connections[i].currBufServer > 0 && fds[2 * i].revents & POLLOUT) {
                        int res = write(connections[i].clientSocket, connections[i].serverBuf,
                                        connections[i].currBufServer);
                        if (res == -1) {
                            connections[i].currBufServer = -1;
                        } else {
                            connections[i].currBufServer = 0;
                        }
                    }
                    if (connections[i].currBufClient > 0 && fds[2 * i + 1].revents & POLLOUT) {
                        int res = write(connections[i].serverSocket, connections[i].clientBuf,
                                        connections[i].currBufClient);
                        if (res == -1) {
                            connections[i].currBufClient = -1;
                        } else {
                            connections[i].currBufClient = 0;
                        }
                    }
                    if ((connections[i].currBufClient < 0 && connections[i].currBufServer <= 0) ||
                        (connections[i].currBufServer < 0 && connections[i].currBufClient <= 0)) {
                        dropConnection(i, "Transmission ended", 1);
                    }
            }
        }
        if (fds[connectionsCount * 2].revents & POLLIN && !(fds[connectionsCount * 2].revents & POLLHUP)) {
            int connectedSocket = accept(listenSocket, (struct sockaddr *) NULL, NULL);
            if (connectedSocket != -1) {
                connections[connectionsCount].clientSocket = connectedSocket;
                connections[connectionsCount].serverSocket = 0;
                connections[connectionsCount].currBufServer = connections[connectionsCount].currBufClient = 0;
                connections[connectionsCount].status = CS_INCOMING;
                connections[connectionsCount].id = rand() % 9000 + 1000;
                connectionsCount++;
            }
        }

    }

    return 0;
}

void form_poll_fds(struct pollfd *fds) {
    for (int i = 0; i < connectionsCount; ++i) {
        fds[i * 2].fd = connections[i].clientSocket;
        fds[i * 2 + 1].fd = connections[i].serverSocket;
        switch (connections[i].status) {
            case CS_INCOMING:
                fds[i * 2].events = POLLIN;
                fds[i * 2 + 1].events = 0;
                break;
            case CS_AUTH_FAILURE:
            case CS_AUTH_SUCCESS:
                fds[i * 2].events = POLLOUT;
                fds[i * 2 + 1].events = 0;
                break;
            case CS_SOCK_REQUEST:
                fds[i * 2].events = POLLIN | POLLOUT;
                fds[i * 2 + 1].events = 0;
                break;
            case CS_RESOLVING:
                fds[i * 2].events = POLLOUT;
                fds[i * 2 + 1].events = 0;
                break;
            case CS_FORWARDING:
                fds[i * 2].events = POLLIN | POLLOUT;
                fds[i * 2 + 1].events = POLLIN | POLLOUT;
                break;
        }
    }
    fds[connectionsCount * 2].fd = listenSocket;
    fds[connectionsCount * 2].events = POLLIN;
    fds[connectionsCount * 2 + 1].fd = dnsSocket;
    fds[connectionsCount * 2 + 1].fd = POLLIN | POLLOUT;
}

void dropConnection(int id, char *reason, int server) {
    printf("Connection %d, %s\n", connections[id].id, reason);
    close(connections[id].clientSocket);
    if (server) {
        close(connections[id].serverSocket);
    }
    connections[id] = connections[connectionsCount - 1];
    connectionsCount--;
}

int sendDNS(int i) {
    size_t length = ((size_t) connections[i].clientBuf[4]);
    int res;
    memcpy(connections[i].serverBuf, connections[i].clientBuf + 5 + length, 2);
    memmove(connections[i].clientBuf, connections[i].clientBuf + 5, length);
    connections[i].clientBuf[length] = '\0';
    if (!(connections[i].R = dns_res_stub(dns_opts(), &res))) {
        fprintf(stderr, "Cannot init DNS resolver: %s\n", dns_strerror(res));
        return 1;
    }
    int error;
    if (error = dns_res_submit(connections[i].R, connections[i].clientBuf, DNS_T_A, DNS_C_IN)) {
        fprintf(stderr, "Resolver error while pushing: %s", dns_strerror(error));
        return 1;
    }
    return 0;
}

char *parseDNS(struct dns_packet *ans) {
    struct dns_a a;
    struct dns_rr rr;
    struct dns_rr_i i;
    int error;
    dns_rr_i_init(memset(&i, 0, sizeof i), ans);
    i.section = DNS_S_ALL & ~DNS_S_QD;
    i.type = DNS_T_A;
    dns_rr_grep(&rr, 1, &i, ans, &error);
    dns_a_parse(&a, &rr, ans);
    printf("%s\n", inet_ntoa(a.addr));
    char *res = malloc(4);
    memcpy(res, &a.addr.s_addr, 4);
    return res;
}

char *errorReply(char code) {
    char *error = (char *) malloc(10);
    memset(error, 0, 10);
    error[0] = 5;
    error[1] = code;
    error[3] = 1;
    return error;
}

char *form_response(int i) {
    if (connections[i].serverSocket == -1) {
        return errorReply(4);
    }
    char *response = (char *) malloc(10);
    response[0] = 5;
    response[1] = 0;
    response[2] = 0;
    response[3] = 1;
    struct sockaddr_in address;
    socklen_t length = sizeof(address);
    getpeername(connections[i].clientSocket, (struct sockaddr *) &address, &length);
    memcpy(response + 4, &address.sin_addr.s_addr, 4);
    memcpy(response + 8, &address.sin_port, 2);
    return response;
}