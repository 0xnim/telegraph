package xyz.nim.telegraph.client;

import xyz.nim.telegraph.client.protocol.CarniteProtocol;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ChannelFilter {
    private static final Duration ACTIVE_THRESHOLD = Duration.ofHours(24);
    private static final Duration NEW_THRESHOLD = Duration.ofHours(1);
    private static final int NEW_MESSAGE_LIMIT = 3;
    private static final int LOW_THRESHOLD = 10;
    private static final int MEDIUM_THRESHOLD = 50;

    private String searchText = "";
    private final Set<ChannelCategory> activeCategories = new HashSet<>();
    private ChannelSortOption sortOption = ChannelSortOption.RECENT_ACTIVITY;
    private boolean includeArchived = false;

    public void setSearchText(String text) {
        this.searchText = text != null ? text.toLowerCase().trim() : "";
    }

    public String getSearchText() {
        return searchText;
    }

    public void toggleCategory(ChannelCategory category) {
        if (activeCategories.contains(category)) {
            activeCategories.remove(category);
        } else {
            activeCategories.add(category);
        }
    }

    public void clearCategories() {
        activeCategories.clear();
    }

    public Set<ChannelCategory> getActiveCategories() {
        return new HashSet<>(activeCategories);
    }

    public void setSortOption(ChannelSortOption option) {
        this.sortOption = option;
    }

    public ChannelSortOption getSortOption() {
        return sortOption;
    }

    public void setIncludeArchived(boolean include) {
        this.includeArchived = include;
    }

    public boolean isIncludeArchived() {
        return includeArchived;
    }

    public List<Integer> apply(TelegraphChannel channel) {
        Set<Integer> allIds = channel.getAllChannelIds();

        return allIds.stream()
            .filter(createFilterPredicate(channel))
            .sorted(createComparator(channel))
            .collect(Collectors.toList());
    }

    private Predicate<Integer> createFilterPredicate(TelegraphChannel channel) {
        return mapId -> {
            if (!includeArchived && channel.isArchived(mapId)) {
                return false;
            }

            if (!searchText.isEmpty()) {
                String displayName = channel.getDisplayName(mapId).toLowerCase();
                List<String> tags = channel.getTags(mapId);
                boolean matchesName = displayName.contains(searchText);
                boolean matchesTags = tags.stream().anyMatch(t -> t.toLowerCase().contains(searchText));
                boolean matchesMapId = String.valueOf(mapId).contains(searchText);

                if (!matchesName && !matchesTags && !matchesMapId) {
                    return false;
                }
            }

            if (!activeCategories.isEmpty() && !activeCategories.contains(ChannelCategory.ALL)) {
                return matchesAnyCategory(mapId, channel);
            }

            return true;
        };
    }

    private boolean matchesAnyCategory(int mapId, TelegraphChannel channel) {
        for (ChannelCategory category : activeCategories) {
            if (matchesCategory(mapId, channel, category)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesCategory(int mapId, TelegraphChannel channel, ChannelCategory category) {
        ChannelSettings settings = channel.getSettings(mapId);
        List<TelegraphMessage> messages = channel.getMessages(mapId);

        return switch (category) {
            case ALL -> true;
            case CARNITE -> settings != null && settings.getProtocol() instanceof CarniteProtocol;
            case TELEGRAPH -> settings != null && !(settings.getProtocol() instanceof CarniteProtocol);
            case ACTIVE -> hasRecentActivity(messages, ACTIVE_THRESHOLD);
            case INACTIVE -> !hasRecentActivity(messages, ACTIVE_THRESHOLD);
            case NEW -> hasRecentActivity(messages, NEW_THRESHOLD) && messages.size() <= NEW_MESSAGE_LIMIT;
            case ARCHIVED -> channel.isArchived(mapId);
            case MUTED -> settings != null && settings.getNotificationLevel() == ChannelSettings.NotificationLevel.NONE;
            case EMPTY -> messages.isEmpty();
            case LOW -> messages.size() >= 1 && messages.size() <= LOW_THRESHOLD;
            case MEDIUM -> messages.size() > LOW_THRESHOLD && messages.size() <= MEDIUM_THRESHOLD;
            case HIGH -> messages.size() > MEDIUM_THRESHOLD;
        };
    }

    private boolean hasRecentActivity(List<TelegraphMessage> messages, Duration threshold) {
        if (messages.isEmpty()) return false;
        Instant cutoff = Instant.now().minus(threshold);
        return messages.stream().anyMatch(m -> m.timestamp().isAfter(cutoff));
    }

    private Comparator<Integer> createComparator(TelegraphChannel channel) {
        return switch (sortOption) {
            case RECENT_ACTIVITY -> (a, b) -> {
                Instant lastA = getLastMessageTime(channel.getMessages(a));
                Instant lastB = getLastMessageTime(channel.getMessages(b));
                return lastB.compareTo(lastA);
            };
            case NAME_ASC -> Comparator.comparing(id -> channel.getDisplayName(id).toLowerCase());
            case NAME_DESC -> (a, b) -> channel.getDisplayName(b).toLowerCase()
                .compareTo(channel.getDisplayName(a).toLowerCase());
            case MAP_ID_ASC -> Comparator.naturalOrder();
            case MAP_ID_DESC -> Comparator.reverseOrder();
            case MESSAGE_COUNT -> (a, b) -> Integer.compare(
                channel.getMessages(b).size(),
                channel.getMessages(a).size()
            );
            case CREATION_DATE -> (a, b) -> {
                Instant firstA = getFirstMessageTime(channel.getMessages(a));
                Instant firstB = getFirstMessageTime(channel.getMessages(b));
                return firstB.compareTo(firstA);
            };
        };
    }

    private Instant getLastMessageTime(List<TelegraphMessage> messages) {
        return messages.stream()
            .map(TelegraphMessage::timestamp)
            .max(Instant::compareTo)
            .orElse(Instant.EPOCH);
    }

    private Instant getFirstMessageTime(List<TelegraphMessage> messages) {
        return messages.stream()
            .map(TelegraphMessage::timestamp)
            .min(Instant::compareTo)
            .orElse(Instant.EPOCH);
    }
}
