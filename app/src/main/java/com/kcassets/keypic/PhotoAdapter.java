package com.kcassets.keypic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class PhotoAdapter extends BaseAdapter {

    Context context;
    LayoutInflater inflater;
    List<String> fileNames;
    List<String> existingFileNames;
    List<String> selectedArray;

    public PhotoAdapter(Context context, List<String> fileNames, List<String> existingFileNames, List<String> selectedArray){
        this.context=context;
        this.fileNames=fileNames;
        this.existingFileNames=existingFileNames;
        this.selectedArray=selectedArray;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return selectedArray.size();
    }

    @Override
    public Object getItem(int i) {
        return selectedArray.get(i);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewgroup) {

        view = inflater.inflate(R.layout.photo_list, null);
        TextView tvPhoto = view.findViewById(R.id.tv_photoName);

        tvPhoto.setText(selectedArray.get(i));

        if (existingFileNames.contains(fileNames.get(i))) {
            view.setBackgroundColor(context.getResources().getColor(R.color.photoColor));
        }

        return view;
    }
}
