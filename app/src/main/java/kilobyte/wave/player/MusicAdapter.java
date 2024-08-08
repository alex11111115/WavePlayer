package kilobyte.wave.player;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import java.util.List;

public class MusicAdapter extends BaseAdapter {

    private final Context context;
    private final List<MusicItem> musicItemList;
    private final OnMusicItemInteractionListener listener;
    private int lastPosition = -1;

    public MusicAdapter(Context context, List<MusicItem> musicItemList, OnMusicItemInteractionListener listener) {
        this.context = context;
        this.musicItemList = musicItemList;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return musicItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return musicItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_music, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        MusicItem musicItem = musicItemList.get(position);
        holder.bind(musicItem, listener);

        setAnimation(holder.cardView, position);

        return convertView;
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.item_animation_fall_down);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    static class ViewHolder {
        CardView cardView;
        ImageView albumArtImageView;
        TextView songTitleTextView;
        TextView artistNameTextView;
        ImageButton playButton;
        ImageButton favoriteButton;

        ViewHolder(View view) {
            cardView = (CardView) view;
            albumArtImageView = view.findViewById(R.id.albumArtImageView);
            songTitleTextView = view.findViewById(R.id.songTitleTextView);
            artistNameTextView = view.findViewById(R.id.artistTextView);
            playButton = view.findViewById(R.id.playButton);
            favoriteButton = view.findViewById(R.id.favoriteButton);
        }

        void bind(final MusicItem musicItem, final OnMusicItemInteractionListener listener) {
            albumArtImageView.setImageResource(musicItem.getAlbumArtResId());
            songTitleTextView.setText(musicItem.getSongTitle());
            artistNameTextView.setText(musicItem.getArtistName());

            playButton.setOnClickListener(v -> {
                Animation animation = AnimationUtils.loadAnimation(v.getContext(), R.anim.button_press);
                v.startAnimation(animation);
                listener.onPlayButtonClicked(musicItem);
            });

            favoriteButton.setOnClickListener(v -> {
                Animation animation = AnimationUtils.loadAnimation(v.getContext(), R.anim.button_press);
                v.startAnimation(animation);
                listener.onFavoriteButtonClicked(musicItem);
            });

            cardView.setOnClickListener(v -> {
                Animation animation = AnimationUtils.loadAnimation(v.getContext(), R.anim.card_click);
                v.startAnimation(animation);
                listener.onMusicItemClicked(musicItem);
            });
        }
    }

    public interface OnMusicItemInteractionListener {
        void onPlayButtonClicked(MusicItem musicItem);
        void onFavoriteButtonClicked(MusicItem musicItem);
        void onMusicItemClicked(MusicItem musicItem);
    }
}