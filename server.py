from bluetooth import * # pybluez
import tkinter
import threading
import struct

class App(threading.Thread):

    def __init__(self):
        threading.Thread.__init__(self)
        self.connected = False
        self.devMac = ""
        self.start()

    def draw(self, macAdr, data):
        self.canvas.delete("all")
        if (len(macAdr) > 1):
            self.canvas.create_text(100, 10, text="connected to:" + macAdr)
        else:
            self.canvas.create_text(30, 30, text="disconnected")
        if (data):
            decoded = data.decode("utf-8")
            if (len(decoded) > 3 and decoded[0] == '>'):
                char = decoded[1]
                decoded = decoded[2:]
                vals = decoded.split(',')
                print(vals)
                vals[0] = round(float(vals[0]), 2)
                vals[1] = round(float(vals[1]), 2)
                vals[2] = round(float(vals[2]), 2)
                text = ""
                if (char == 'A'):
                    text = "Accelometer"
                else:
                    text = "Gyroscope"
                self.canvas.create_text(80, 20, text=text, fill='blue', justify='left')

                self.canvas.create_text(20, 30, text=vals[0], fill='red')
                self.canvas.create_rectangle(40, 30, 40 + 100*vals[0], 35, fill='red')

                self.canvas.create_text(20, 40, text=vals[0], fill='green')
                self.canvas.create_rectangle(40, 40, 40 + 100*vals[1], 45, fill='green')

                self.canvas.create_text(20, 50, text=vals[0], fill='blue')
                self.canvas.create_rectangle(40, 50, 40 + 100*vals[2], 55, fill='blue')
                self.canvas.pack()

    def callback(self):
        self.canvas.quit()

    def run(self):
        self.canvas = tkinter.Canvas()
        self.canvas.pack()
        self.canvas.mainloop()


app = App()


server_sock=BluetoothSocket( RFCOMM )
server_sock.bind(("",PORT_ANY))
server_sock.listen(1)

port = server_sock.getsockname()[1]

uuid = "72764c49-a85c-4780-8767-b20da73e1bd4"

advertise_service( server_sock, "SampleServer",
                   service_id = uuid,
                   service_classes = [ uuid, SERIAL_PORT_CLASS ],
                   profiles = [ SERIAL_PORT_PROFILE ], 
                    )
                   
print("Waiting for connection on RFCOMM channel %d" % port)

client_sock, client_info = server_sock.accept()
macAdr = client_info[0]
print(client_info[0])
print("Accepted connection from ", client_info)

try:
    while True:
        data = client_sock.recv(1024)
        print(data)
        if len(data) == 0: break
        app.draw(macAdr, data)
except IOError:
    pass

app.draw("", None)
print("disconnected")

client_sock.close()
server_sock.close()
print("all done")

