package com.applikey.mattermost.adapters.channel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.applikey.mattermost.R;
import com.applikey.mattermost.adapters.channel.viewholder.BaseChatListViewHolder;
import com.applikey.mattermost.adapters.channel.viewholder.GroupChatListViewHolder;
import com.applikey.mattermost.adapters.channel.viewholder.UserChatListViewHolder;
import com.applikey.mattermost.models.channel.Channel;
import com.applikey.mattermost.web.images.ImageLoader;

import io.realm.OrderedRealmCollection;
import io.realm.RealmResults;

public class UnreadChatListAdapter extends BaseChatListAdapter<BaseChatListViewHolder> {

    public UnreadChatListAdapter(@NonNull Context context, RealmResults<Channel> data,
                                 ImageLoader imageLoader, String currentUserId) {
        super(context, data, imageLoader, currentUserId);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public int getItemViewType(int position) {
        final OrderedRealmCollection<Channel> data = getData();

        final Channel channel = data.get(position);

        if (Channel.ChannelType.fromRepresentation(channel.getType()) == Channel.ChannelType.DIRECT) {
            return R.layout.list_item_chat;
        } else {
            return R.layout.list_item_group_chat;
        }
    }

    @Override
    public BaseChatListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view;
        final BaseChatListViewHolder viewHolder;
        final Context context = parent.getContext();
        if (viewType == R.layout.list_item_group_chat) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_group_chat, parent, false);
            viewHolder = new GroupChatListViewHolder(view, mCurrentUserId);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_chat, parent, false);
            viewHolder = new UserChatListViewHolder(view, mCurrentUserId);
        }

        viewHolder.getContainer().setOnClickListener(mOnClickListener);
        return viewHolder;
    }
}
