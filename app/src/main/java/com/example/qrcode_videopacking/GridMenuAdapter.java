package com.example.qrcode_videopacking;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class GridMenuAdapter extends ArrayAdapter<GridMenuModel> {
    private ArrayList<GridMenuModel> dataSet;
    private Context mContext;

    public static class ViewHolder {
        public TextView txtTitle;
        public ImageView imgIcon;
    }

    public GridMenuAdapter(Context context, ArrayList<GridMenuModel> data) {
        super(context, -1, data);
        this.dataSet = data;
        this.mContext=context;

    }

    private int lastPosition = -1;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final GridMenuModel dataModel = getItem(position);
        final ViewHolder viewHolder; // view lookup cache stored in tag
        final LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        viewHolder = new ViewHolder();
        convertView = inflater.inflate(R.layout.gridmenu, parent, false);
        viewHolder.txtTitle = (TextView) convertView.findViewById(R.id.gridTitle);
        viewHolder.imgIcon = (ImageView) convertView.findViewById(R.id.gridImage);
        convertView.setTag(viewHolder);

        lastPosition = position;
        viewHolder.txtTitle.setText(dataModel.getTitle());
        viewHolder.imgIcon.setImageResource(dataModel.getIcon());
        return convertView;
    }
}
