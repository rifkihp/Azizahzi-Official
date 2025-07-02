package com.example.qrcode_videopacking;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        final GridView gridMenuView = (GridView) findViewById(R.id.gridMenu);
        final ArrayList<GridMenuModel> gridMenu = new ArrayList<>();
        gridMenu.add(new GridMenuModel("Menu 1", R.drawable.info_circle));
        gridMenu.add(new GridMenuModel("Menu 2", R.drawable.key));
        gridMenu.add(new GridMenuModel("Menu 3", R.drawable.luggage_cart));
        gridMenu.add(new GridMenuModel("Menu 4", R.drawable.minus_circle));
        gridMenu.add(new GridMenuModel("Menu 5", R.drawable.pallet));
        gridMenu.add(new GridMenuModel("Menu 6", R.drawable.receipt));
        gridMenu.add(new GridMenuModel("Menu 7", R.drawable.receive));
        gridMenu.add(new GridMenuModel("Menu 8", R.drawable.recycle));
        gridMenu.add(new GridMenuModel("Menu 9", R.drawable.return_truck));
        gridMenu.add(new GridMenuModel("Menu 10", R.drawable.sign_out_alt));
        gridMenu.add(new GridMenuModel("Menu 11", R.drawable.stock));
        gridMenu.add(new GridMenuModel("Menu 12", R.drawable.term));
        gridMenu.add(new GridMenuModel("Menu 13", R.drawable.truck));
        gridMenu.add(new GridMenuModel("Menu 14", R.drawable.user_circle));

        final GridMenuAdapter adapter = new GridMenuAdapter(getApplicationContext(), gridMenu);
        gridMenuView.setAdapter(adapter);
        gridMenuView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                GridMenuModel dataModel= gridMenu.get(position);
                Snackbar.make(view, dataModel.getTitle() +" Pos:" + position, Snackbar.LENGTH_SHORT)
                        .setAction("No action", null).show();
            }
        });

    }
}
