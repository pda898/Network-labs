#include "socketUtils.h"

int openListenSocket(uint16_t port) {
    struct sockaddr_in result;
    result.sin_addr.s_addr = htonl(INADDR_ANY);
    result.sin_family = AF_INET;
    result.sin_port = htons(port);
    int listenSocket = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC | SOCK_NONBLOCK, 0);
    if (bind(listenSocket, (struct sockaddr *) &result, sizeof(result))) {
        fprintf(stderr, "Error: cannot bind socket\n");
        exit(1);
    }
    if (listen(listenSocket, 100)) {
        fprintf(stderr, "Error: cannot listen socket\n");
    }
    return listenSocket;
}

int openServerSocket(char *address_in, char *port) {
    struct sockaddr_in address;
    address.sin_family = AF_INET;
    memcpy(&address.sin_addr.s_addr, address_in, 4);
    printf("%s\n", inet_ntoa(address.sin_addr));
    memcpy(&address.sin_port, port, 2);
    int clientSocket = socket(AF_INET, SOCK_STREAM, 0);
    if (clientSocket < 0) {
        fprintf(stderr, "Error: cannot create socket\n");
    }
    if (connect(clientSocket, (struct sockaddr *) &address, sizeof(address)) < 0) {
        if (errno != EINPROGRESS) {
            perror("Error: cannot connect");
        }
    }
    return clientSocket;
}