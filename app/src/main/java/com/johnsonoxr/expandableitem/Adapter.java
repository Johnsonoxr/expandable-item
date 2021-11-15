package com.johnsonoxr.expandableitem;

import android.content.Context;
import android.util.ArraySet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.ChangeBounds;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;


public class Adapter extends RecyclerView.Adapter<Adapter.VH> {

    public interface ItemListener {
        void onItemClick(Item item);

        void onCheckboxChanged(Item item, boolean checked);
    }

    private interface RefreshAction {
        void invoke(VH holder);
    }

    private final List<Item> mItems;
    private Item mFocus;
    private boolean mIsCheckboxVisible;

    private RecyclerView mRcv;
    private final Set<VH> mHolderSet = new ArraySet<>();
    private final ItemListener mItemListener;
    private final int itemMaxHeight;
    private final int marginVertical;
    private final int marginHorizontal;

    final ConstraintSet collapsedConstraintSet;
    final ConstraintSet expandedConstraintSet;

    public Adapter(Context context, ItemListener itemListener) {
        mItemListener = itemListener;
        mItems = IntStream.range(0, 30).mapToObj(i -> new Item()).collect(Collectors.toList());
        marginHorizontal = context.getResources().getDimensionPixelSize(R.dimen.item_margin_horizontal);
        marginVertical = context.getResources().getDimensionPixelSize(R.dimen.item_margin_vertical);
        itemMaxHeight = context.getResources().getDimensionPixelSize(R.dimen.item_default_height)
                + context.getResources().getDimensionPixelSize(R.dimen.item_expand_height)
                + 2 * marginVertical;

        //  These lines are somehow quite expansive on computing, so we do it here instead of doing it in VH.setExpand() each time.
        collapsedConstraintSet = new ConstraintSet();
        collapsedConstraintSet.clone(context, R.layout.item);
        expandedConstraintSet = new ConstraintSet();
        expandedConstraintSet.clone(context, R.layout.item_expand);
    }

    public Item getFocus() {
        return mFocus;
    }

    public List<Item> getItems() {
        return mItems;
    }

    public void removeItem(Item item) {
        int position = mItems.indexOf(item);
        if (position < 0) {
            return;
        }
        if (item == mFocus) {
            mFocus = null;
        }
        refresh(item, holder -> TransitionManager.beginDelayedTransition(mRcv, new TransitionSet().setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(new Slide(Gravity.END).addTarget(holder.itemView))
                .addTransition(new ChangeBounds().setStartDelay(250))));
        mItems.remove(item);
        notifyItemRemoved(position);
    }

    public void focus(Item item) {
        if (mFocus == item) {
            return;
        }
        if (mFocus != null) {
            refresh(mFocus, holder -> holder.setExpand(false, true));
        }
        if (item != null) {
            refresh(item, holder -> holder.setExpand(true, true));
        }
        mFocus = item;
    }

    public void showCheckbox(boolean show) {
        mIsCheckboxVisible = show;
        focus(null);
        refreshAll(holder -> holder.setShowCheckbox(show, true));
    }

    public boolean isCheckboxVisible() {
        return mIsCheckboxVisible;
    }

    private void refresh(Item item, RefreshAction action) {
        int position = mItems.indexOf(item);
        if (position < 0) {
            return;
        }
        mHolderSet.stream().filter(h -> h.getAdapterPosition() == position).findFirst().ifPresent(action::invoke);
    }

    private void refreshAll(RefreshAction action) {
        mHolderSet.stream().filter(vh -> vh.getAdapterPosition() >= 0).forEach(action::invoke);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        mRcv = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        mRcv = recyclerView;
        mHolderSet.clear();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        mHolderSet.add(holder);
        Item item = mItems.get(position);
        holder.bg.setBackgroundColor(item.color);
        holder.date.setText(SimpleDateFormat.getDateInstance().format(new Date(item.date)));
        holder.setExpand(mFocus == item, false);
        holder.setShowCheckbox(mIsCheckboxVisible, false);
    }

    @Override
    public void onViewRecycled(@NonNull VH holder) {
        mHolderSet.remove(holder);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public class VH extends RecyclerView.ViewHolder implements CompoundButton.OnCheckedChangeListener {

        final View bg;
        final CheckBox cb;
        final ImageView iv;
        final View title;
        final TextView date;
        final View share;
        final View camera;
        final ConstraintLayout detail;

        public VH(@NonNull View itemView) {
            super(itemView);
            bg = itemView.findViewById(R.id.bg);
            cb = itemView.findViewById(R.id.cb);
            iv = itemView.findViewById(R.id.iv);
            title = itemView.findViewById(R.id.title);
            date = itemView.findViewById(R.id.date);
            share = itemView.findViewById(R.id.share);
            camera = itemView.findViewById(R.id.camera);
            detail = itemView.findViewById(R.id.detail);

            cb.setOnCheckedChangeListener(this);
            bg.setOnClickListener(v -> {
                Item item = mItems.get(getAdapterPosition());
                if (mIsCheckboxVisible) {
                    cb.setChecked(!cb.isChecked());
                } else {
                    mItemListener.onItemClick(item);
                }
            });
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Item item = mItems.get(getAdapterPosition());
            item.selected = isChecked;
            mItemListener.onCheckboxChanged(item, isChecked);
        }

        public void setExpand(boolean expand, boolean animation) {
            if (animation) {
                TransitionManager.beginDelayedTransition(mRcv, new AutoTransition().setOrdering(TransitionSet.ORDERING_TOGETHER));
            }

            ConstraintSet constraintSet = expand ? expandedConstraintSet : collapsedConstraintSet;
            constraintSet.applyTo((ConstraintLayout) itemView);

            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();
            if (expand) {
                layoutParams.setMargins(0, 0, 0, 0);
            } else {
                layoutParams.setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical);
            }

            //  Keep the whole item in view when focusing and animating.
            if (expand && animation) {
                if (itemView.getTop() < 0) {
                    mRcv.scrollToPosition(getAdapterPosition());
                } else if (itemView.getTop() + itemMaxHeight > mRcv.getHeight()) {
                    LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mRcv.getLayoutManager();
                    if (linearLayoutManager != null) {
                        linearLayoutManager.scrollToPositionWithOffset(getAdapterPosition(), mRcv.getHeight() - itemMaxHeight);
                    }
                }
            }
        }

        public void setShowCheckbox(boolean show, boolean animation) {
            Item item = mItems.get(getAdapterPosition());
            if (show) {
                cb.setOnCheckedChangeListener(null);
                cb.setChecked(item.selected);
                cb.setOnCheckedChangeListener(this);
                cb.jumpDrawablesToCurrentState();
            } else {
                item.selected = false;
            }

            if (animation) {
                TransitionManager.beginDelayedTransition(mRcv, new AutoTransition().setOrdering(TransitionSet.ORDERING_TOGETHER));
            }

            cb.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
