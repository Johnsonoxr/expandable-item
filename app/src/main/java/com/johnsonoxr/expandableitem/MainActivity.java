package com.johnsonoxr.expandableitem;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity implements Adapter.ItemListener {

    private Adapter mAdapter;
    private FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        setSupportActionBar(findViewById(R.id.toolbar));

        RecyclerView rcv = findViewById(R.id.rcv);

        mAdapter = new Adapter(this, this);
        rcv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false) {
            final int expandHeight = getResources().getDimensionPixelSize(R.dimen.item_expand_height);

            //  This would keep the item on the edge, which might be slightly out of screen, being considered as visible,
            //  and animate as a "ChangeBound" but not "Fade" when expanding/collapsing items.
            @Override
            protected void calculateExtraLayoutSpace(@NonNull RecyclerView.State state, @NonNull int[] extraLayoutSpace) {
                super.calculateExtraLayoutSpace(state, extraLayoutSpace);
                for (int i = 0; i < 2; i++) {
                    if (extraLayoutSpace[i] == 0) {
                        extraLayoutSpace[i] = expandHeight;
                    }
                }
            }
        });
        rcv.setItemAnimator(null);  //  make our own remove animation.
        rcv.setAdapter(mAdapter);

        mFab = findViewById(R.id.fab);
        mFab.setOnClickListener(v -> {
            if (mAdapter.isCheckboxVisible()) {
                List<Item> selectedItems = mAdapter.getItems().stream().filter(item -> item.selected).collect(Collectors.toList());
                selectedItems.forEach(mAdapter::removeItem);
                //  Don't know how but this works.
                mFab.post(() -> {
                    mAdapter.showCheckbox(false);
                    updateFab();
                });
            } else {
                mAdapter.showCheckbox(true);
                updateFab();
            }
        });
    }

    @Override
    public void onItemClick(Item item) {
        if (mAdapter.getFocus() == item) {
            mAdapter.focus(null);
        } else {
            mAdapter.focus(item);
        }
    }

    @Override
    public void onCheckboxChanged(Item item, boolean checked) {
        updateFab();
    }

    private void updateFab() {
        mFab.setImageResource(!mAdapter.isCheckboxVisible() ? android.R.drawable.ic_menu_edit
                : mAdapter.getItems().stream().anyMatch(i -> i.selected) ? android.R.drawable.ic_menu_delete
                : android.R.drawable.ic_menu_revert);
    }
}