package com.example.schoolmanagement;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;

public class RecentChatAdapter extends BaseAdapter {

    Context context;
    List<RecentChatModel> chatList;

    public RecentChatAdapter(Context context, List<RecentChatModel> chatList) {
        this.context = context;
        this.chatList = chatList;
    }

    @Override
    public int getCount() { return chatList.size(); }

    @Override
    public Object getItem(int position) { return chatList.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.recent_chat_item, parent, false);

        RecentChatModel model = chatList.get(position);

        TextView nameTv = convertView.findViewById(R.id.chatName);
        TextView lastMsgTv = convertView.findViewById(R.id.lastMsg);

        nameTv.setText(model.getName());
        lastMsgTv.setText(model.getLastMsg());

        return convertView;
    }
}
