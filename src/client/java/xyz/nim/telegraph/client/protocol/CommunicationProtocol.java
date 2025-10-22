package xyz.nim.telegraph.client.protocol;

import xyz.nim.telegraph.client.TelegraphMessage;

import java.util.List;

public interface CommunicationProtocol {
    String getName();
    
    String getDescription();
    
    List<String> getChannelTypes();
    
    String formatMessage(TelegraphMessage message, String channelType);
    
    int getColorForChannelType(String channelType);
    
    String getChannelTypeDescription(String channelType);
}
