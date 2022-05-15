from socket import socket
from socket import AF_INET
from socket import SOCK_STREAM
from socket import SHUT_RDWR

# Class that implements the Client side of the socket communication to the 
# Java server
class Client:

    # Empty init file is needed to treat file correctly as package
    def __init__(self):
        pass

    """
    Connects client to Server.
    ARGS:
        port (int):     The port on which to connect to the server.
        host (string):  The host name ('127.0.0.1' is default).
    RETURN:
        bool: True if connection successful, else False
    """
    def connect(self, port: int, host: str = '127.0.0.1'):
        # AF_INET     = Address family (IPv4)
        # SOCK_STREAM = Socket type
        self.client = socket(AF_INET, SOCK_STREAM) # Create a socket
        self.client.connect((host, port)) # Connect to host on given port

    """
    Send message to host.
    In Pythonic fashion, we assume the user has already established connection.
    ARGS:
        msg (str):   Message to be sent.
        delim (str): The string to end the msg
    """
    def send(self, msg: str, delim: str = '\n'):
        # Send message to server as an encoded byte array where the msg is encoded using ascii 
        # (UTF-8 and other encodings should work as well).
        # \n has to be there as an end tag
        self.client.sendall(bytes(msg + delim, 'ASCII'))
        
    """
    Wait for message from host.
    In Pythonic fashion, we assume the user has already established connection.
    ARGS:
        MSGLEN (int): The maximum number of bytes to receive
        delim (str):  The delim on which to end the receive
    RETURN:
        String: Message string from host.
    """
    def receive(self, MSGLEN: int = 16, delim: bytes = b'\n') -> str:
        msg = b''
        # Maximum number of bytes to receive
        while len(msg) < MSGLEN:
            data = self.client.recv(1)
            # If we receive the stopping delim: Stop receiving data.
            if data == delim: 
                break
            msg += data

        # return string decoded by same convention as above (no real difference using utf-8).
        return msg.decode('ASCII') 

    """
    Correctly closes communication to host (assuming we have connected correctly).
    """
    def close(self):
        # Shutdown tells the other side of the connection that we are no longer sending/receiving
        self.client.shutdown(SHUT_RDWR)
        self.client.close()

"""
Test script to show how this can be used as an echo-client.
"""
if __name__ == '__main__':
    msg = 'Echo'
    try:
        c = Client()
        c.connect(4444) # IP-address implicit
        print('Connection to host established')
        print('Sending message: {}'.format(msg))
        c.send(msg)
        echo = c.receive()
        print('Echo received: {}'.format(echo))
    except:
        print('Something failed')
    finally:
        print('Closing client connection')
        c.close()

