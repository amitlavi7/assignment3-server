package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.impl.stomp.framesReceived.*;
import bgu.spl.net.impl.stomp.framesToSend.ConnectedCommand;
import bgu.spl.net.impl.stomp.framesToSend.Error;
import bgu.spl.net.impl.stomp.framesToSend.Message;
import bgu.spl.net.impl.stomp.framesToSend.Receipt;
import bgu.spl.net.srv.Frame;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;

public class StompMessageEncoderDecoder implements MessageEncoderDecoder<Frame<String>> {

    private byte[] bytes = new byte[1 << 10];
    private int len = 0;
    private LinkedList<String> words = new LinkedList<>();

    @Override
    public Frame<String> decodeNextByte(byte nextByte) {
        if (nextByte == '\u0000') {
            words.addLast(popString()); //words.add
            return buildFrame();
        }
        if (nextByte == '\n') {
            words.addLast(popString()); //words.add
        }
        pushByte (nextByte);
        return null;
    }

    @Override
    public byte[] encode(Frame<String> frame) {
          StringBuilder frameToBytes = new StringBuilder();
          switch (frame.getOpCode()) {
              case (1): { //connected
                  ConnectedCommand command = ((ConnectedCommand)frame);
                  frameToBytes.append(command.getConnected()).append("\n").append(command.getVersion()).append("\n");
                  break;
              }
              case (2): { //error
                  Error command = ((Error)frame);
                  frameToBytes.append(command.getError()).append("\n").append(command.getCauseOfError()).append("\n");
                  break;
              }
              case  (3): { //message
                  Message command = ((Message)frame);
                  frameToBytes.append(command.getMessgae()).append("\n").
                          append(command.getSub()).append("\n").
                          append(command.getId()).append("\n").
                          append(command.getDes()).append("\n").
                          append(command.getBody()).append("\n");
                  break;
              }
              case (4): { // receipt
                  Receipt command = ((Receipt)frame);
                  frameToBytes.append(command.getReceipt()).append("\n").append(command.getId()).append("\n");
              }
          }
        return (frameToBytes.toString() + '\u0000').getBytes();
    }

    private void pushByte (byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len++] = nextByte;

    }

    private String popString() {
        String res = new String(bytes,0,len);
        len = 0;
        return res;
    }

    private Frame<String> buildFrame(){
        switch (words.getFirst()){
            case ("CONNECT"): {
                String version = words.get(1).split(":")[1];
                String host = words.get(2).split(":")[1];
                String login = words.get(3).split(":")[1];
                String pass = words.get(4).split(":")[1];
                words.clear();
                return new ConnectCommand(version,host ,login ,pass);
            }
            case ("SUBSCRIBE"): {
                String destination = words.get(1).split(":")[1];
                String id = words.get(2).split(":")[1];
                String receipt = words.get(3).split(":")[1];
                words.clear();
                return new SubscribeCommand(destination, id, receipt);
            }
            case ("SEND"): {
                String destination = words.get(1).split(":")[1];;
                String body = words.get(3);
                words.clear();
                return new SendCommand(destination,body);
            }
            case ("DISCONNECT"): {
                String receipt = words.get(1).split(":")[1];
                words.clear();
                return new DisconnectCommand(receipt);
            }
            case ("UNSUBSCRIBE"):{
                String id = words.get(1).split(":")[1];
                String receipt = words.get(2).split(":")[1];
                words.clear();
                return new Unsubscribe(id, receipt);
            }
            default: return null;
        }
    }
}
