package uk.co.speedfox.appliancefinder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class ApplianceListAdapter extends BaseAdapter {
    HashMap<String, String> appliances;

    public ApplianceListAdapter(){
        appliances = new HashMap<>();
    }

    @Override
    public int getCount() {
        return appliances.size();
    }

    @Override
    public Object getItem(int i) {
        return  appliances.entrySet().toArray()[i];
    }

    public String getIpAddress(int i) {
        return ((Map.Entry<String, String>)appliances.entrySet().toArray()[i]).getKey();
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View oldView, ViewGroup viewGroup) {
        Map.Entry<String, String> entry = (Map.Entry<String, String>) appliances.entrySet().toArray()[i];
        entry.getValue();

        final View result;

        if (oldView == null) {
            result = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item, viewGroup, false);
        } else {
            result = oldView;
        }

        ((TextView) result.findViewById(android.R.id.text1)).setText(entry.getValue());
        return result;
    }

    public void addEntry(String key, String value){
        value = String.format("%s (%s)", value, key);
        if(!appliances.containsKey(key)){
            appliances.put(key, value);
            notifyDataSetChanged();
        }
    }

    public void clear(){
        appliances.clear();
        notifyDataSetChanged();
    }
}
