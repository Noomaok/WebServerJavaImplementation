all: serv client

serv:
	javac -d bin src/http/server/WebServer.java

client:
	javac -d bin src/http/client/WebPing.java