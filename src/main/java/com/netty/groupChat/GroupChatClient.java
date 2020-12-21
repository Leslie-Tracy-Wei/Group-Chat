package com.netty.groupChat;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Scanner;

public class GroupChatClient {
    private final static String ADDRESS = "127.0.0.1";
    private final static Integer PORT = 9999;
    private Selector selector;
    private SocketChannel socketChannel;
    private String userName;

    public GroupChatClient(){

        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open(new InetSocketAddress(ADDRESS,PORT));
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            userName = socketChannel.getLocalAddress().toString().substring(1);
            System.out.println(userName + " is OK...");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendInfo(String info) {
        info = LocalDateTime.now() + " " + userName + " send: " + info;

        try {
            socketChannel.write(ByteBuffer.wrap(info.getBytes()));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void readInfo(){
        try {
            int readChannel = selector.select();
            // 有可用的通道
            if (readChannel > 0){
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while(iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    if (key.isReadable()){
                        // 等到相关的通道
                        SocketChannel channel = (SocketChannel)key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int read = channel.read(buffer);
                        if (read > 0){
                            String msg = new String(buffer.array());
                            System.out.println(msg.trim());
                        }
                    }
                    // 删掉用过的key 避免重复使用
                    iterator.remove();
                }
            } else{
                System.out.println("无可用通道");
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        GroupChatClient groupChatClient = new GroupChatClient();


        // 启动一个线程 每隔3m ，读取从服务器端发送的数据
        new Thread(() -> {
            while (true){
                groupChatClient.readInfo();
                try {
                    Thread.sleep(3000);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

        Scanner sn = new Scanner(System.in);

        while(sn.hasNextLine()){
            String s = sn.nextLine();
            groupChatClient.sendInfo(s);
        }
    }
}
