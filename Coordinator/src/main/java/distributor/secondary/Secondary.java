package distributor.secondary;

import error.WrongMessageTypeException;
import message.Message;
import message.MessageTypes;
import message.msgconstructor.CheckPointConstructor;
import message.msgconstructor.MemberShipConstructor;
import org.json.JSONObject;
import services.common.NetServiceFactory;
import services.common.NetServiceProxy;
import services.io.NetConfig;
import shared.AllSecondaries;
import shared.AllSlaves;
import shared.ConnMetrics;
import distributor.Distributer;
import shared.Job;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Created by xingchij on 11/19/15.
 */
public class Secondary extends Distributer {
    private DatagramSocket getCheckPointDock;

    NetServiceProxy checkPointService = NetServiceFactory.getCheckPointService();

    public Secondary(String id) throws SocketException, UnknownHostException {
        ip = NetConfig.getMyIp();
        this.id = id;
        coordinator = new NetConfig(IPOfCoordinator, portOfCoordinatorHeartBeat);

        slaveOffice = AllSlaves.getOffice();
        backUps = AllSecondaries.getInstance();

        getCheckPointDock = new DatagramSocket(ConnMetrics.portOfSecondaryCheckPoint);

        getCheckPointDock.setSoTimeout(100);
    }

    public void serve() {

        try{
            checkPointing();
        }catch (IOException e){
            e.printStackTrace();
        } catch (WrongMessageTypeException e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }

    }


    public void closeConnections() {
        if(getCheckPointDock!=null) {
            getCheckPointDock.close();
        }
    }

    /**
     *  used by RunMain
     *
     * @param msg
     * @return
     */
    @Override
    public boolean dealWithMemberShipMsg(Message msg) {
        if(msg.getType() != MessageTypes.MEMBERSHIP){
            System.out.printf("Receive wrong type from membership dock: %d\n", msg.getType());
            return false;
        }
        String content = msg.getContent();

        JSONObject json = new JSONObject(content);

        String type = json.getString("type");

        if(type.equals(MemberShipConstructor.NEWSECONDARY)){
            String id = json.getString("id");
            String ip = json.getString("ip");
            try {
                backUps.addSecondary(id, ip, portOfSecondaryCheckPoint);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return false;
            }
        }else if(type.equals(MemberShipConstructor.SECONDARYDEAD)){
            String id = json.getString("id");
            backUps.delSecondary(id);
            System.out.printf("Secondary %s dead\n", id);
        }else{
            System.out.println("Un-acceptable membership message");
            System.out.println(msg);
            return false;
        }
        return true;
    }

    private boolean checkPointing() throws IOException, WrongMessageTypeException {
        Message check = checkPointService.recvAckMessage(getCheckPointDock);

        if(check == null)
            return true;

        System.out.println(check);

        if(check.getType() != MessageTypes.CHECKPOINT){
            throw new WrongMessageTypeException(check.getType(), MessageTypes.CHECKPOINT);
        }

        String content = check.getContent();

        JSONObject json = new JSONObject(content);
        String type = json.getString("checktype");

        if(type == null){
            System.out.println("Err: Bad CheckPoint Message");
            return false;
        }
        if(type.equals(CheckPointConstructor.ADD_JOB)){
            JSONObject jobjson = json.getJSONObject("jobDetail");
            String id = json.getString("sid");

            Job newJob= new Job(jobjson);
            slaveOffice.checkAddNewJob(id, newJob);
        }else if(type.equals(CheckPointConstructor.ADD_SLAVE)){
            String sid = json.getString("sid");
            String ip = json.getString("ip");
            if(sid == null || ip == null){
                System.out.printf(
                        "Err: CheckPoint add slave, but missing critical information[id: %s, ip: %s]\n",
                        sid, ip);
                return false;
            }
            slaveOffice.addSlave(sid, ip);

        }else if(type.equals(CheckPointConstructor.SET_JOB_STATUS)){
            String sid = json.getString("sid");
            String jid = json.getString("jobid");
            String status = json.getString("status");

            slaveOffice.setJobStatus(sid, jid, status);
        }else{
            System.out.println("Err: Unknown CheckPoint Type");
            return false;
        }
        return true;
    }
}