package services.io;

import message.Message;

import java.io.IOException;
import java.net.DatagramSocket;

/**
 * Created by xingchij on 11/17/15.
 */
public interface NetService {
	public boolean sendMessage(Message msg, DatagramSocket serverSocket,
			NetConfig netConf) throws IOException;

	public Message receiveMessage(DatagramSocket serverSocket)
			throws IOException;

	public Message recvAckMessage(DatagramSocket serverSocket)
			throws IOException;
}
