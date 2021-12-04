package com.hiddenramblings.tagmo.adapter;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.hiddenramblings.tagmo.R;
import com.hiddenramblings.tagmo.TagMo;
import com.hiddenramblings.tagmo.amiibo.Amiibo;
import com.hiddenramblings.tagmo.nfctech.TagUtils;
import com.hiddenramblings.tagmo.settings.BrowserSettings;
import com.hiddenramblings.tagmo.settings.BrowserSettings.BrowserSettingsListener;
import com.hiddenramblings.tagmo.settings.BrowserSettings.VIEW;
import com.hiddenramblings.tagmo.widget.BoldSpannable;

import java.util.ArrayList;

public class BankBrowserAdapter
        extends RecyclerView.Adapter<BankBrowserAdapter.AmiiboViewHolder>
        implements BrowserSettingsListener {

    private final BrowserSettings settings;
    private final OnAmiiboClickListener listener;
    private ArrayList<Amiibo> amiibos = new ArrayList<>();

    public BankBrowserAdapter(BrowserSettings settings, OnAmiiboClickListener listener) {
        this.settings = settings;
        this.listener = listener;
    }

    public void setAmiibos(ArrayList<Amiibo> amiibos) {
        this.amiibos = amiibos;
    }

    @Override
    public void onBrowserSettingsChanged(BrowserSettings newBrowserSettings, BrowserSettings oldBrowserSettings) {

    }

    @Override
    public int getItemCount() {
        return amiibos.size();
    }

    @Override
    public long getItemId(int i) {
        return amiibos.get(i).id;
    }

    public Amiibo getItem(int i) {
        return amiibos.get(i);
    }

    @Override
    public int getItemViewType(int position) {
        return settings.getAmiiboView();
    }

    @NonNull
    @Override
    public AmiiboViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (VIEW.valueOf(viewType)) {
            case COMPACT:
                return new CompactViewHolder(parent, settings, listener);
            case LARGE:
                return new LargeViewHolder(parent, settings, listener);
            case IMAGE:
                return new ImageViewHolder(parent, settings, listener);
            case SIMPLE:
            default:
                return new SimpleViewHolder(parent, settings, listener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final AmiiboViewHolder holder, int position) {
        View highlight = holder.itemView.findViewById(R.id.highlight);
        if (TagMo.getPrefs().eliteActiveBank().get() == position) {
            highlight.setBackgroundColor(ContextCompat.getColor(TagMo.getContext(),
                    TagMo.isDarkTheme() ? android.R.color.holo_green_dark
                            : android.R.color.holo_green_light));
        } else {
            highlight.setBackgroundColor(ContextCompat.getColor(
                    TagMo.getContext(), android.R.color.transparent));
        }
        holder.itemView.setOnClickListener(view -> {
            if (null != holder.listener)
                holder.listener.onAmiiboClicked(holder.amiiboItem, position);
        });
        if (null != holder.imageAmiibo) {
            holder.imageAmiibo.setOnClickListener(view -> {
                if (null != holder.listener) {
                    if (settings.getAmiiboView() == VIEW.IMAGE.getValue())
                        holder.listener.onAmiiboClicked(holder.amiiboItem, position);
                    else
                        holder.listener.onAmiiboImageClicked(holder.amiiboItem, position);
                }
            });
        }
        holder.itemView.setOnLongClickListener(view -> {
            if (null != holder.listener)
                return holder.listener.onAmiiboLongClicked(holder.amiiboItem, position);
            return false;
        });
        holder.bind(getItem(position));
    }

    protected static abstract class AmiiboViewHolder extends RecyclerView.ViewHolder {

        private final BrowserSettings settings;
        private final OnAmiiboClickListener listener;

        public final TextView txtError;
        public final TextView txtName;
        public final TextView txtTagId;
        public final TextView txtAmiiboSeries;
        public final TextView txtAmiiboType;
        public final TextView txtGameSeries;
        // public final TextView txtCharacter;
        public final TextView txtPath;
        public final ImageView imageAmiibo;

        Amiibo amiiboItem = null;

        private final BoldSpannable boldSpannable = new BoldSpannable();

        CustomTarget<Bitmap> target = new CustomTarget<Bitmap>() {
            @Override
            public void onLoadStarted(@Nullable Drawable placeholder) { }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                imageAmiibo.setVisibility(View.GONE);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) { }

            @Override
            public void onResourceReady(@NonNull Bitmap resource, Transition transition) {
                imageAmiibo.setImageBitmap(resource);
                imageAmiibo.setVisibility(View.VISIBLE);
            }
        };

        public AmiiboViewHolder(View itemView, BrowserSettings settings,
                                OnAmiiboClickListener listener) {
            super(itemView);

            this.settings = settings;
            this.listener = listener;
            this.txtError = itemView.findViewById(R.id.txtError);
            this.txtName = itemView.findViewById(R.id.txtName);
            this.txtTagId = itemView.findViewById(R.id.txtTagId);
            this.txtAmiiboSeries = itemView.findViewById(R.id.txtAmiiboSeries);
            this.txtAmiiboType = itemView.findViewById(R.id.txtAmiiboType);
            this.txtGameSeries = itemView.findViewById(R.id.txtGameSeries);
            // this.txtCharacter = itemView.findViewById(R.id.txtCharacter);
            this.txtPath = itemView.findViewById(R.id.txtPath);
            this.imageAmiibo = itemView.findViewById(R.id.imageAmiibo);
        }

        @SuppressLint("SetTextI18n")
        void bind(final Amiibo amiibo) {
            this.amiiboItem = amiibo;

            String amiiboHexId = "";
            String amiiboName = "";
            String amiiboSeries = "";
            String amiiboType = "";
            String gameSeries = "";
            // String character = "";
            String amiiboImageUrl = null;
            boolean isAmiibo = null != amiibo;

            if (isAmiibo) {
                this.amiiboItem.bank = getAbsoluteAdapterPosition();
                amiiboHexId = TagUtils.amiiboIdToHex(amiibo.id);
                amiiboImageUrl = amiibo.getImageUrl();
                if (null != amiibo.name)
                    amiiboName = amiibo.name;
                if (null != amiibo.getAmiiboSeries())
                    amiiboSeries = amiibo.getAmiiboSeries().name;
                if (null != amiibo.getAmiiboType())
                    amiiboType = amiibo.getAmiiboType().name;
                if (null != amiibo.getGameSeries())
                    gameSeries = amiibo.getGameSeries().name;
                // if (null != amiibo.getCharacter())
                //     gameSeries = amiibo.getCharacter().name;
            }

            String query = settings.getQuery().toLowerCase();
            String value = String.valueOf(getAbsoluteAdapterPosition() + 1);

            if (settings.getAmiiboView() != VIEW.IMAGE.getValue()) {
                this.txtError.setVisibility(View.GONE);
                if (isAmiibo) {
                    setAmiiboInfoText(this.txtName, value + ": " + amiiboName);
                    setAmiiboInfoText(this.txtTagId, boldSpannable.StartsWith(amiiboHexId, query));
                    setAmiiboInfoText(this.txtAmiiboSeries,
                            boldSpannable.IndexOf(amiiboSeries, query));
                    setAmiiboInfoText(this.txtAmiiboType,
                            boldSpannable.IndexOf(amiiboType, query));
                    setAmiiboInfoText(this.txtGameSeries,
                            boldSpannable.IndexOf(gameSeries, query));
                    // setAmiiboInfoText(this.txtCharacter,
                    // boldText.Matching(character, query));
                } else {
                    this.txtName.setVisibility(View.VISIBLE);
                    this.txtName.setText(TagMo.getStringRes(R.string.blank_bank, value));
                    this.txtTagId.setVisibility(View.GONE);
                    this.txtAmiiboSeries.setVisibility(View.GONE);
                    this.txtAmiiboType.setVisibility(View.GONE);
                    this.txtGameSeries.setVisibility(View.GONE);
                    // this.txtCharacter.setVisibility(View.GONE);
                }
            }

            if (null != this.imageAmiibo) {
                this.imageAmiibo.setVisibility(View.GONE);
                Glide.with(itemView).clear(target);
                Glide.with(itemView)
                        .setDefaultRequestOptions(settings.onlyRetrieveFromCache(itemView))
                        .asBitmap()
                        .load(null != amiiboImageUrl ? amiiboImageUrl: R.mipmap.ic_launcher)
                        .thumbnail(0.25f)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(target);
            }
        }

        private void setAmiiboInfoText(TextView textView, CharSequence text) {
            textView.setVisibility(View.VISIBLE);
            if (text.length() == 0) {
                textView.setText(R.string.unknown);
                textView.setEnabled(false);
            } else {
                textView.setText(text);
                textView.setEnabled(true);
            }
        }
    }

    static class SimpleViewHolder extends AmiiboViewHolder {
        public SimpleViewHolder(ViewGroup parent, BrowserSettings settings,
                                OnAmiiboClickListener listener) {
            super(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.amiibo_simple_card, parent, false),
                    settings, listener
            );
        }
    }

    static class CompactViewHolder extends AmiiboViewHolder {
        public CompactViewHolder(ViewGroup parent, BrowserSettings settings,
                                 OnAmiiboClickListener listener) {
            super(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.amiibo_compact_card, parent, false),
                    settings, listener
            );
        }
    }

    static class LargeViewHolder extends AmiiboViewHolder {
        public LargeViewHolder(ViewGroup parent, BrowserSettings settings,
                               OnAmiiboClickListener listener) {
            super(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.amiibo_large_card, parent, false),
                    settings, listener
            );
        }
    }

    static class ImageViewHolder extends AmiiboViewHolder {
        public ImageViewHolder(ViewGroup parent, BrowserSettings settings,
                               OnAmiiboClickListener listener) {
            super(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.amiibo_image_card, parent, false),
                    settings, listener
            );
        }
    }

    public interface OnAmiiboClickListener {
        void onAmiiboClicked(Amiibo amiibo, int position);

        void onAmiiboImageClicked(Amiibo amiibo, int position);

        boolean onAmiiboLongClicked(Amiibo amiibo, int position);
    }
}