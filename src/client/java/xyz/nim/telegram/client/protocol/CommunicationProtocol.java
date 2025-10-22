package xyz.nim.telegram.client.protocol;

import xyz.nim.telegram.client.TelegramMessage;

import java.util.List;

public interface CommunicationProtocol {
    String getName();
    
    String getDescription();
    
    List<String> getChannelTypes();
    
    String formatMessage(TelegramMessage message, String channelType);
    
    int getColorForChannelType(String channelType);
    
    String getChannelTypeDescription(String channelType);
}
