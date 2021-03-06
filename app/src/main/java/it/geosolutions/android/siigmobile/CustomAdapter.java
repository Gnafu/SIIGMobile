package it.geosolutions.android.siigmobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.TreeSet;

/**
 * Created by Lorenzo on 21/10/2015.
 */

class CustomAdapter extends BaseAdapter {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_HEADER = 1;
    private static final int TYPE_SEPARATOR = 2;

    private ArrayList<String> mData = new ArrayList<String>();
    private TreeSet<Integer> sectionHeader = new TreeSet<Integer>();
    private TreeSet<Integer> sectionSeparator = new TreeSet<Integer>();

    private LayoutInflater mInflater;

    public CustomAdapter(Context context) {
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void addItem(final String item) {
        mData.add(item);
        notifyDataSetChanged();
    }

    public void addSectionHeaderItem(final String item) {
        mData.add(item);
        sectionHeader.add(mData.size() - 1);
        notifyDataSetChanged();
    }

    public void addSeparatorItem() {
        mData.add("");
        sectionSeparator.add(mData.size() - 1);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return sectionHeader.contains(position) ? TYPE_HEADER :
                (sectionSeparator.contains(position) ? TYPE_SEPARATOR : TYPE_ITEM);
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public String getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        int rowType = getItemViewType(position);

        if (convertView == null) {
            holder = new ViewHolder();
            switch (rowType) {
                case TYPE_ITEM:
                    convertView = mInflater.inflate(R.layout.drawer_layout_item, null);
                    holder.textView = (TextView) convertView.findViewById(R.id.label);
                    break;
                case TYPE_HEADER:
                    convertView = mInflater.inflate(R.layout.drawer_layout_sectionheader, null);
                    holder.textView = (TextView) convertView.findViewById(R.id.textHeader);
                    break;
                case TYPE_SEPARATOR:
                    convertView = mInflater.inflate(R.layout.drawer_layout_separator, null);
                    holder.separatorView = convertView.findViewById(R.id.separatorView);
                    break;
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if(holder.textView != null) {
            holder.textView.setText(mData.get(position));
        }

        return convertView;
    }

    public static class ViewHolder {
        public TextView textView;
        public View separatorView;
    }

}