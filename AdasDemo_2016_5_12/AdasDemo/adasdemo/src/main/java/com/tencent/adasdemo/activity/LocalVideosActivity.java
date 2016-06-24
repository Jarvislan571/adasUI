package com.tencent.adasdemo.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.tencent.adasdemo.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地视频列表
 */
public class LocalVideosActivity extends Activity {

    List<String> paths = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_videos);
        ListView list_files = (ListView) findViewById(R.id.list_files);
        String path = Environment.getExternalStorageDirectory()
                + File.separator + "adas";
        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    paths.add(files[i].getAbsolutePath());
                }
            }
        }
        list_files.setAdapter(new ArrayAdapter<String>(getApplicationContext(),
                R.layout.list_files_item,
                R.id.text_item,
                paths
        ));
        list_files.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(LocalVideosActivity.this, AdasActivity.class);
                intent.putExtra("path", paths.get(position));
                startActivity(intent);
            }
        });
    }
}
