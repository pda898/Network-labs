#include "includes.h"

#define BUFFER_SIZE 4096

enum connectionStatus {
    CS_INCOMING = 0,
    CS_AUTH_SUCCESS = 8,
    CS_AUTH_FAILURE = 9,
    CS_SOCK_REQUEST = 10,
    CS_RESOLVING = 11,
    CS_FORWARDING = 12
};

struct connection {
    int clientSocket, serverSocket;
    enum connectionStatus status;
    ssize_t currBufServer, currBufClient;
    int id;
    char clientBuf[BUFFER_SIZE];
    char serverBuf[BUFFER_SIZE];
    struct dns_resolver *R;
};