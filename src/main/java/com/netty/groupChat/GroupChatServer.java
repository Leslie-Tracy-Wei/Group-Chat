package com.netty.groupChat;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * 聊天服务器
 */
public class GroupChatServer {
    private Selector selector;
    private ServerSocketChannel listenChannel;

    private static final int PORT = 9999;
    public GroupChatServer(){
        // 初始化工作
        try {
            // 等到选择器
            selector = Selector.open();
            // 等到channel
            listenChannel = ServerSocketChannel.open();
            // 绑定端口
            listenChannel.socket().bind(new InetSocketAddress(PORT));
            // 设置为非阻塞
            listenChannel.configureBlocking(false);
            // 注册到selector上
            listenChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // 监听
    public void listen(){
        try {
            // 循环处理
            while (true){
                int count = selector.select();
                // 如果大于0，说明有时间处理
                if (count > 0){
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while(iterator.hasNext()){
                        SelectionKey key = iterator.next();

                        // 不同的key有不同的处理方式
                        if (key.isAcceptable()){
                            SocketChannel socketChannel = listenChannel.accept();
                            socketChannel.configureBlocking(false);
                            System.out.println("C端连接过来 " + socketChannel.getRemoteAddress().toString());
                            // 将socketChannel 注册到selector
                            socketChannel.register(selector,SelectionKey.OP_READ);

                            System.out.println(socketChannel.getRemoteAddress() + "上线啦");
                        }
                        // 通道发生read时间 ，通道为可读的状态
                        if (key.isReadable()){
                            // 处理读的方法
                            handleRead(key);
                        }
                        // 处理当前的key 反正重复处理
                        iterator.remove();
                    } 
                } else{
                    System.out.println("服务器空闲，无连接");
                }
            }

        } catch (Exception e){
            e.printStackTrace();
        } finally {

        }
    }

    /**
     * 读出客户端消息
     * @param key
     */
    private void handleRead(SelectionKey key) {
        SocketChannel channel = null;

        try{
            // 获得channel
            channel = (SocketChannel)key.channel();
            // 创建buffer
            ByteBuffer bu = ByteBuffer.allocate(1024);
            int read = channel.read(bu);
            if (read > 0){
                String acceptMsg = new String(bu.array());
                System.out.println("from C端：" + acceptMsg);

                // 向其他服务端发送消息
                sendInfo2OtherClients(acceptMsg,channel);
            }
        }catch (IOException e){
            try{
                System.out.println(channel.getRemoteAddress() + " 离线了 ");
                // 取消注册
                key.cancel();
                // 关闭通道
                channel.close();
            } catch (IOException e1){
                e1.printStackTrace();
            }

        }
    }

    // 转发消息给其他客户 (通道)
    private void sendInfo2OtherClients(String msg,SocketChannel self) throws IOException{
        System.out.println("服务器转发消息中...");

        // 遍历 所有注册到selector上的SocketChannel 并排除 self
        for (SelectionKey key : selector.keys()) {
            Channel channel = key.channel();
            if (channel instanceof SocketChannel && channel != self){
                SocketChannel dest =  (SocketChannel) channel;
                ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                // 将buffer的数据写入通道
                dest.write(buffer);
            }
        }
    }

    public static void main(String[] args) {

        GroupChatServer groupChatServer = new GroupChatServer();
        groupChatServer.listen();
    }
}
