all: proxy

proxy: dns.o socketUtils.o proxy.o
	gcc -o proxy dns.o socketUtils.o proxy.o
	
dns.o: dns.c dns.c
	gcc -c dns.c
	
socketUtils.o: socketUtils.c socketUtils.h includes.h
	gcc -c socketUtils.c
	
proxy.o: proxy.c includes.h connection.h
	gcc -c proxy.c
	
clean:
	rm proxy
	rm *.o