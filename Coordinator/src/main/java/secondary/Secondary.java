package secondary;

import message.Message;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import shared.AllSecondaries;
import shared.AllSlaves;
import shared.ConnMetrics;
import shared.Distributer;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by xingchij on 11/19/15.
 */
public class Secondary extends Distributer {
    private DatagramSocket getCheckPointDock;

    NetServiceProxy checkPointServive = NetServiceFactory.getCheckPointService();

    public Secondary() throws SocketException {
        slaveOffice = AllSlaves.getOffice();
        backUps = AllSecondaries.getInstance();

        getCheckPointDock = new DatagramSocket(ConnMetrics.portOfSecondaryCheckPoint);

        getCheckPointDock.setSoTimeout(100);
    }
    @Override
    public void serve() {


        try{
            Message check = checkPointServive.recvAckMessage(getCheckPointDock);
            if(check != null) {
                System.out.println(check);
            }
        }catch (IOException e){
            e.printStackTrace();
        }


    }

    public void closeConnections() {
        if(getCheckPointDock!=null) {
            getCheckPointDock.close();
        }

    }
}
