package kilobyte.wave.player;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import sound.wave.kilobyte.WavePlayerView;

public class MainActivity extends AppCompatActivity implements MusicAdapter.OnMusicItemInteractionListener {
	
	private MotionLayout motionLayout;
	private WavePlayerView waveView;
	private GridView musicGridView;
	private View fullPlayerLayout, miniPlayerLayout;
	private ImageButton btnCollapse, btnShuffle, btnPrevious, btnNext, btnRepeat, miniPlayerPlayPause, miniPlayerSkipNext, volume;
	private ShapeableImageView ivAlbumArt, miniPlayerAlbumArt;
	private TextView tvSongTitle, tvArtist, tvCurrentTime, tvTotalTime, miniPlayerSongTitle, miniPlayerArtist;
	private Slider seekBar;
	private FloatingActionButton btnPlayPause;
	
	private static final int PERMISSION_REQUEST_CODE = 1;
	private MusicAdapter musicAdapter;
	private List<MusicItem> musicItemList;
	
	private float initialTouchY;
	private float fullPlayerInitialHeight;
	private boolean isAnimating = false;
	
	private final Handler handler = new Handler(Looper.getMainLooper());
	private String audioFilePath = "";
	private double seekPosition = 0;
	private float playbackSpeed = 1.0f;
	boolean isUserInteracting = false;
	private static final String TAG = "MainActivity";
	private boolean isPlay = false;
	private boolean isRepeat = false;
	private MusicItem filee;
	
	private ActivityResultLauncher<String[]> permissionLauncher;
	
	private boolean isShuffleOn = false;
	private List<MusicItem> originalPlaylist;
	private List<MusicItem> shuffledPlaylist;
	private Random random = new Random();
	private int Volume = 100;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		initializeViews();
		setupListeners();
		setupCollapseButton();
		setupPermissionLauncher();
		checkPermissionAndLoadMusic();
		
		waveView.setOnAudioCompleteListener(() -> {
			seekBar.setValue(0);
			updateCurrentTimeText(0);
			if(isRepeat) {
				playAudioFromFile(filee);
			} else {
				btnPlayPause.setImageResource(R.drawable.ic_pause);
				miniPlayerPlayPause.setImageResource(R.drawable.ic_pause);
				isPlay = false;
			}
		});
	}
	
	private void initializeViews() {
		// تعريف جميع العناصر في الواجهة
		motionLayout = findViewById(R.id.motionLayout);
		waveView = findViewById(R.id.waveView);
		musicGridView = findViewById(R.id.musicGridView);
		fullPlayerLayout = findViewById(R.id.fullPlayerLayout);
		btnCollapse = findViewById(R.id.btnCollapse);
		ivAlbumArt = findViewById(R.id.ivAlbumArt);
		tvSongTitle = findViewById(R.id.tvSongTitle);
		tvArtist = findViewById(R.id.tvArtist);
		seekBar = findViewById(R.id.seekBar);
		tvCurrentTime = findViewById(R.id.tvCurrentTime);
		tvTotalTime = findViewById(R.id.tvTotalTime);
		btnShuffle = findViewById(R.id.btnShuffle);
		btnPrevious = findViewById(R.id.btnPrevious);
		btnPlayPause = findViewById(R.id.btnPlayPause);
		btnNext = findViewById(R.id.btnNext);
		btnRepeat = findViewById(R.id.btnRepeat);
		volume = findViewById(R.id.volume);
		miniPlayerLayout = findViewById(R.id.miniPlayerLayout);
		miniPlayerAlbumArt = findViewById(R.id.miniPlayerAlbumArt);
		miniPlayerSongTitle = findViewById(R.id.miniPlayerSongTitle);
		miniPlayerArtist = findViewById(R.id.miniPlayerArtist);
		miniPlayerPlayPause = findViewById(R.id.miniPlayerPlayPause);
		miniPlayerSkipNext = findViewById(R.id.miniPlayerSkipNext);
		
		musicItemList = new ArrayList<>();
		musicAdapter = new MusicAdapter(this, musicItemList, this);
		musicGridView.setAdapter(musicAdapter);
	}
	
	private void setupListeners() {
		// إعداد المستمعين لجميع الأزرار والعناصر التفاعلية
		btnCollapse.setOnClickListener(v -> collapseFullPlayer());
		miniPlayerPlayPause.setOnClickListener(v -> togglePlayPause());
		btnPlayPause.setOnClickListener(v -> togglePlayPause());
		btnNext.setOnClickListener(v -> skipToNext());
		btnPrevious.setOnClickListener(v -> skipToPrevious());
		btnShuffle.setOnClickListener(v -> toggleShuffle());
		btnRepeat.setOnClickListener(v -> toggleRepeat());
		miniPlayerSkipNext.setOnClickListener(v -> skipToNext());
		volume.setOnClickListener(v -> controlVolume());
		
		setupSeekBarListener();
		setupFullPlayerTouchListener();
		setupMiniPlayerTouchListener();
	}
	
	private void setupSeekBarListener() {
		seekBar.addOnChangeListener((slider, value, fromUser) -> {
			if (fromUser) {
				updateCurrentTimeText((int) value);
				if (!isUserInteracting) {
					seekToPosition((int) value);
				}
			}
		});
		
		seekBar.setOnTouchListener((v, event) -> {
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
				isUserInteracting = true;
				if (isPlay) {
					waveView.pauseAudioWithWave();
				}
				return true;
				case MotionEvent.ACTION_UP:
				isUserInteracting = false;
				seekToPosition((int) seekBar.getValue());
				if (isPlay) {
					waveView.resumeAudioWithWave();
				}
				return true;
			}
			return false;
		});
	}
	
	private void setupFullPlayerTouchListener() {
		fullPlayerLayout.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
					initialTouchY = event.getRawY();
					fullPlayerInitialHeight = fullPlayerLayout.getHeight();
					return true;
					case MotionEvent.ACTION_MOVE:
					float deltaY = event.getRawY() - initialTouchY;
					if (deltaY > 0 && !isAnimating) {
						float progress = Math.min(1, deltaY / fullPlayerInitialHeight);
						updatePlayerLayouts(progress); 
					}
					return true;
					case MotionEvent.ACTION_UP:
					float finalDeltaY = event.getRawY() - initialTouchY;
					if (finalDeltaY > fullPlayerInitialHeight / 2) {
						animateToMiniPlayer();
					} else {
						animateToFullPlayer();
					}
					return true;
				}
				return false;
			}
		});
	}
	
	private void setupMiniPlayerTouchListener() {
		miniPlayerLayout.setOnTouchListener((v, event) -> {
			if (event.getAction() == MotionEvent.ACTION_UP) {
				animateToFullPlayer();
				return true;
			}
			return false;
		});
	}
	
	private void setupPermissionLauncher() {
		permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
			boolean allGranted = true;
			for (Boolean isGranted : permissions.values()) {
				if (!isGranted) {
					allGranted = false;
					break;
				}
			}
			if (allGranted) {
				loadMusic();
			} else {
				showPermissionDeniedMessage();
			}
		});
	}
	
	private void setupCollapseButton() {
		ImageButton btnCollapse = findViewById(R.id.btnCollapse);
		btnCollapse.setOnClickListener(v -> animateToMiniPlayer());
	}
	
	private void setupAudioPlayer() {
		waveView.setOnAudioProgressListener(this::updateProgress);
		handler.removeCallbacks(updateProgressRunnable);
		handler.post(updateProgressRunnable);
	}
	
	private void updatePlayerLayouts(float progress) {
		float inverseProgress = 1 - progress;
		
		// تحديث حجم البلاير الكبير
		ViewGroup.LayoutParams fullPlayerParams = fullPlayerLayout.getLayoutParams();
		fullPlayerParams.height = (int) (fullPlayerInitialHeight * inverseProgress);
		fullPlayerLayout.setLayoutParams(fullPlayerParams);
		
		// تحديث الشفافية
		fullPlayerLayout.setAlpha(inverseProgress);
		miniPlayerLayout.setAlpha(progress);
		
		// تحديث الرؤية
		fullPlayerLayout.setVisibility(inverseProgress > 0 ? View.VISIBLE : View.GONE);
		miniPlayerLayout.setVisibility(progress > 0 ? View.VISIBLE : View.GONE);
	}
	
	private void updatePlayPauseButton() {
		int resourceId = isPlay ? R.drawable.ic_pause : R.drawable.ic_play;
		btnPlayPause.setImageResource(resourceId);
		miniPlayerPlayPause.setImageResource(resourceId);
	}
	
	private void updateRepeatButton() {
		int resourceId = isRepeat ? R.drawable.ic_repeated : R.drawable.ic_repeat;
		btnRepeat.setImageResource(resourceId);
	}
	
	private void updatePlayerPosition(int position) {
		tvCurrentTime.setText(formatMilliseconds(position));
	}
	
	private void updatePlayerWithMusicItem(MusicItem musicItem) {
		tvSongTitle.setText(musicItem.getSongTitle());
		tvArtist.setText(musicItem.getArtistName());
		ivAlbumArt.setImageResource(musicItem.getAlbumArtResId());
		miniPlayerSongTitle.setText(musicItem.getSongTitle());
		miniPlayerArtist.setText(musicItem.getArtistName());
		miniPlayerAlbumArt.setImageResource(musicItem.getAlbumArtResId());
		seekBar.setValueTo(musicItem.getDuration());
		tvTotalTime.setText(formatMilliseconds(musicItem.getDuration()));
		
		animationW();
		filee = musicItem;
		playAudioFromFile(musicItem);
		
		isPlay = true;
	}
	
	private void updateProgress(int currentTime) {
		runOnUiThread(() -> {
			if (!isUserInteracting) {
				updateCurrentTimeText(currentTime);
				seekBar.setValue(currentTime);
			}
		});
	}
	
	private void updateDuration() {
		int duration = waveView.getAudioDuration();
		tvCurrentTime.setText(formatMilliseconds(duration));
		seekBar.setValue(duration);
	}
	
	private void updateShuffleButton() {
		int color = isShuffleOn ? Color.parseColor("#8e8de5") : Color.WHITE;
		btnShuffle.setColorFilter(color);
	}
	
	private void updatePlaybackSpeed() {
		try {
			waveView.setPlaybackSpeed(String.valueOf(playbackSpeed));
		} catch (Exception e) {
			Log.e(TAG, "Error updating playback speed", e);
			Toast.makeText(this, "فشل في تحديث سرعة التشغيل", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void updateCurrentTimeText(int milliseconds) {
		tvCurrentTime.setText(formatMilliseconds(milliseconds));
	}
	
	private void togglePlayPause() {
		if (audioFilePath.isEmpty()) {
			Toast.makeText(this, "الرجاء اختيار أغنية أولاً", Toast.LENGTH_SHORT).show();
			return;
		}
		
		if (isPlay) {
			pauseAudio();
		} else {
			resumeAudio();
		}
		isPlay = !isPlay;
		updatePlayPauseButton();
	}
	
	private void toggleRepeat() {
		isRepeat = !isRepeat;
		if (waveView != null) {
			waveView.setLooping(isRepeat);
		}
		updateRepeatButton();
	}
	
	private void toggleShuffle() {
		isShuffleOn = !isShuffleOn;
		if (isShuffleOn) {
			enableShuffle();
		} else {
			disableShuffle();
		}
		updateShuffleButton();
	}
	
	private void loadMusic() {
		Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		String[] projection = {
			MediaStore.Audio.Media.TITLE,
			MediaStore.Audio.Media.ARTIST,
			MediaStore.Audio.Media.ALBUM_ID,
			MediaStore.Audio.Media.DURATION,
			MediaStore.Audio.Media.DATA
		};
		
		try (Cursor cursor = getContentResolver().query(musicUri, projection, null, null, null)) {
			if (cursor != null && cursor.getCount() > 0) {
				while (cursor.moveToNext()) {
					String songTitle = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
					String artistName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
					int albumId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
					int duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
					String data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
					
					// التحقق من صحة البيانات قبل إنشاء MusicItem
					if (songTitle != null && !songTitle.isEmpty() && data != null && !data.isEmpty()) {
						int albumArtResId = getAlbumArtResId(albumId);
						
						MusicItem musicItem = new MusicItem.Builder()
						.setSongTitle(songTitle)
						.setArtistName(artistName != null ? artistName : "مجهول")
						.setAlbumArtResId(albumArtResId)
						.setDuration(duration)
						.setFilePath(data)
						.build();
						
						musicItemList.add(musicItem);
					}
				}
				if (!musicItemList.isEmpty()) {
					musicAdapter.notifyDataSetChanged();
				} else {
					showEmptyMusicLibraryMessage();
				}
			} else {
				showEmptyMusicLibraryMessage();
			}
		} catch (Exception e) {
			Log.e(TAG, "Error loading music", e);
			Toast.makeText(this, "حدث خطأ أثناء تحميل الموسيقى", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void showEmptyMusicLibraryMessage() {
		Toast.makeText(this, "لم يتم العثور على ملفات موسيقية", Toast.LENGTH_LONG).show();
	}
	
	private int getAlbumArtResId(int albumId) {
		Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
		Uri albumUri = Uri.withAppendedPath(albumArtUri, String.valueOf(albumId));
		return albumUri != null ? R.drawable.default_album_art : R.drawable.default_album_art;
	}
	
	private String formatMilliseconds(int milliseconds) {
		int seconds = (milliseconds / 1000) % 60;
		int minutes = (milliseconds / (1000 * 60)) % 60;
		int hours = (milliseconds / (1000 * 60 * 60)) % 24;
		
		return hours > 0 ? String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
		: String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
	}
	
	private void collapseFullPlayer() {
		fullPlayerLayout.setVisibility(View.GONE);
		miniPlayerLayout.setVisibility(View.VISIBLE);
	}
    
    private void playAudioFromFile(MusicItem musicItem) {
		audioFilePath = musicItem.setFilePath();
		if (!audioFilePath.isEmpty()) {
			try {
				setupAudioPlayer();
				waveView.playAudioWithWaveFromPath(audioFilePath);
				updatePlaybackSpeed();
				btnPlayPause.setImageResource(R.drawable.ic_pause);
				miniPlayerPlayPause.setImageResource(R.drawable.ic_pause);
				isPlay = true;
			} catch (Exception e) {
				Log.e(TAG, "Error playing audio from file", e);
				Toast.makeText(this, "فشل في تشغيل الملف الصوتي", Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(this, "قد لا يكون ملف الموسيقى موجودًا أو تالفًا", Toast.LENGTH_SHORT).show();
		}
	}
    
    private final Runnable updateProgressRunnable = new Runnable() {
		@Override
		public void run() {
			if (isPlay) {
				int currentPosition = waveView.getCurrentAudioPosition();
				updateProgress(currentPosition);
				handler.postDelayed(this, 50);
			}
		}
	};
	
	private void animateToFullPlayer() {
		if (isAnimating) return;
		isAnimating = true;
		
		ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
		animator.setDuration(300);
		animator.setInterpolator(new AccelerateDecelerateInterpolator());
		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				float progress = (float) animation.getAnimatedValue();
				updatePlayerLayouts(progress);
			}
		});
		animator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				isAnimating = false;
			}
		});
		animator.start();
	}
	
	private void animateToMiniPlayer() {
		if (isAnimating) return;
		isAnimating = true;
		
		// إخفاء البلاير الكبير
		fullPlayerLayout.animate()
		.alpha(0f)
		.translationY(fullPlayerLayout.getHeight())
		.setDuration(300)
		.setInterpolator(new AccelerateInterpolator())
		.setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				fullPlayerLayout.setVisibility(View.GONE);
				isAnimating = false;
			}
		});
		
		// إظهار البلاير الصغير
		miniPlayerLayout.setVisibility(View.VISIBLE);
		miniPlayerLayout.setAlpha(0f);
		miniPlayerLayout.setTranslationY(miniPlayerLayout.getHeight());
		miniPlayerLayout.animate()
		.alpha(1f)
		.translationY(0)
		.setDuration(300)
		.setInterpolator(new DecelerateInterpolator())
		.setListener(null);
	}
	
	private void animationW(){
		fullPlayerLayout.setAlpha(0f);
		fullPlayerLayout.setVisibility(View.VISIBLE);
		fullPlayerLayout.animate()
		.alpha(1f)
		.setDuration(300)
		.setListener(null);
		
		miniPlayerLayout.animate()
		.alpha(0f)
		.setDuration(300)
		.setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				miniPlayerLayout.setVisibility(View.GONE);
			}
		});
	}
	
	private void skipToNext() {
		int currentIndex = musicItemList.indexOf(filee);
		if (currentIndex < musicItemList.size() - 1) {
			MusicItem nextItem = musicItemList.get(currentIndex + 1);
			updatePlayerWithMusicItem(nextItem);
		} else {
			Toast.makeText(this, "هذه آخر أغنية في القائمة", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void skipToPrevious() {
		int currentIndex = musicItemList.indexOf(filee);
		if (currentIndex > 0) {
			MusicItem previousItem = musicItemList.get(currentIndex - 1);
			updatePlayerWithMusicItem(previousItem);
		} else {
			Toast.makeText(this, "هذه أول أغنية في القائمة", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void seekToPosition(int position) {
		if (waveView != null) {
			waveView.seekToPosition(position);
			updateCurrentTimeText(position);
		}
	}
	
	private void enableShuffle() {
		if (originalPlaylist == null) {
			originalPlaylist = new ArrayList<>(musicItemList);
		}
		shuffledPlaylist = new ArrayList<>(originalPlaylist);
		Collections.shuffle(shuffledPlaylist, random);
		musicItemList.clear();
		musicItemList.addAll(shuffledPlaylist);
		musicAdapter.notifyDataSetChanged();
	}
	
	private void disableShuffle() {
		if (originalPlaylist != null) {
			musicItemList.clear();
			musicItemList.addAll(originalPlaylist);
			musicAdapter.notifyDataSetChanged();
		}
	}
	
	private void pauseAudio() {
		waveView.pauseAudioWithWave();
		updateCurrentTimeText((int) seekBar.getValue());
	}
	
	private void resumeAudio() {
		waveView.resumeAudioWithWave();
		setupAudioPlayer();
	}
	
	private void controlVolume(){
		if(Volume == 0 ) {
			Volume = 100;
			waveView.setVolume(Volume, Volume);
			volume.setImageResource(R.drawable.ic_volume);
		} else {
			Volume = 0;
			waveView.setVolume(Volume, Volume);
			volume.setImageResource(R.drawable.ic_volume_mute);
		}
	}
	
	private void setPlaybackSpeed() {
		try {
			updatePlaybackSpeed();
		} catch (NumberFormatException e) {
			Toast.makeText(this, "صيغة السرعة غير صالحة", Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public void onPlayButtonClicked(MusicItem musicItem) {
		updatePlayerWithMusicItem(musicItem);
	}
	
	@Override
	public void onFavoriteButtonClicked(MusicItem musicItem) {
		// التعامل مع نقرة زر المفضلة
	}
	
	@Override
	public void onMusicItemClicked(MusicItem musicItem) {
		updatePlayerWithMusicItem(musicItem);
	}
	
	private void checkPermissionAndLoadMusic() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) 
			!= PackageManager.PERMISSION_GRANTED) {
				permissionLauncher.launch(new String[]{Manifest.permission.READ_MEDIA_AUDIO});
			} else {
				loadMusic();
			}
		} else {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
			!= PackageManager.PERMISSION_GRANTED) {
				permissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
			} else {
				loadMusic();
			}
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == PERMISSION_REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				loadMusic();
			} else {
				showPermissionDeniedMessage();
			}
		}
	}
	
	private void showPermissionDeniedMessage() {
		// عرض رسالة للمستخدم تشرح سبب الحاجة إلى الإذن
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (waveView != null) {
			waveView.release();
		}
		handler.removeCallbacksAndMessages(null);
	}
}