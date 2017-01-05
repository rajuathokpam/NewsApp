package com.itservz.paomacha.android.fragment;

import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.itservz.paomacha.android.PaoActivity;
import com.itservz.paomacha.android.R;
import com.itservz.paomacha.android.backend.FirebaseDatabaseService;
import com.itservz.paomacha.android.model.Pao;
import com.itservz.paomacha.android.preference.PrefManager;
import com.itservz.paomacha.android.utils.DownloadImageTask;
import com.itservz.paomacha.android.utils.Share;
import com.itservz.paomacha.android.view.ActionBarToggler;


/**
 * Fragment to manage the central page of the 5 pages application navigation (top, center, bottom, left, right).
 */
public class CentralFragment extends Fragment {
    static final String TAG = "CentralFragment";
    private PrefManager prefManager;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private PaoActivity paoActivity = null;
    private Pao pao = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        pao = (Pao) this.getArguments().getSerializable("paof");
        Log.d(TAG, pao.toString());
        pao.uuid.trim();
        final View fragmentView = inflater.inflate(R.layout.fragment_central, container, false);
        paoActivity = (PaoActivity) getActivity();

        ImageView paopic = (ImageView) fragmentView.findViewById(R.id.paopic);
        if (pao.imageUrl != null) {
            new DownloadImageTask(paopic).execute(pao.imageUrl);
        } else {
            if (pao.image != null) {
                byte[] bytes = pao.image.getBytes();
                paopic.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            }
        }

        TextView title = (TextView) fragmentView.findViewById(R.id.title);
        title.setText(pao.title);
        TextView body = (TextView) fragmentView.findViewById(R.id.body);
        body.setText(pao.body);
        TextView footer = (TextView) fragmentView.findViewById(R.id.footer);
        footer.setText("paoap by " + pao.createdBy + " / " + pao.createdOn);

        addListeners(fragmentView);
        return fragmentView;
    }

    private void addListeners(final View fragmentView) {
        fragmentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppBarLayout appBarLayout = (AppBarLayout) paoActivity.findViewById(R.id.appbar);
                View toolbarBottom = fragmentView.findViewById(R.id.toolbarBottom);
                View fab = paoActivity.findViewById(R.id.fab);
                if (paoActivity.FULLSCREEN) {
                    Log.d(TAG, "fragmentView.setOnClickListener show");
                    ActionBarToggler.showAppBar(appBarLayout);
                    ActionBarToggler.showAppBar(toolbarBottom);
                    ActionBarToggler.showAppBar(fab);
                    paoActivity.FULLSCREEN = false;
                } else {
                    Log.d(TAG, "fragmentView.setOnClickListener hide");
                    ActionBarToggler.hideAppBar(appBarLayout);
                    ActionBarToggler.hideBottomBar(toolbarBottom, fab);
                    paoActivity.FULLSCREEN = true;
                }

            }
        });

        prefManager = new PrefManager(paoActivity);
        final ImageButton bookmark = (ImageButton) fragmentView.findViewById(R.id.bookmark);
        bookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (prefManager.hasBookmark(pao.uuid)) {
                    bookmark.setImageDrawable(getResources().getDrawable(R.drawable.ic_bookmark_black_24dp));
                    prefManager.removeBookmark(pao.uuid);
                } else {
                    bookmark.setImageDrawable(getResources().getDrawable(R.drawable.ic_bookmark_border_black_24dp));
                    prefManager.addBookmark(pao.uuid);
                }
            }
        });

        final View likeContainer = fragmentView.findViewById(R.id.like_container);
        final ImageButton like = (ImageButton) fragmentView.findViewById(R.id.like);
        final TextView likeCount = (TextView) fragmentView.findViewById(R.id.like_count);
        likeCount.setText("" + pao.likes);
        likeContainer.setOnClickListener(new View.OnClickListener() {
            int count = pao.likes;
            @Override
            public void onClick(View view) {
                if (prefManager.hasLike(pao.uuid)) {//had already liked, now undo
                    like.setImageDrawable(getResources().getDrawable(R.drawable.ic_thumb_up_p_24dp));
                    count--;
                    likeCount.setText(""+count);
                    prefManager.removeLike(pao.uuid);
                } else { //liking it
                    like.setImageDrawable(getResources().getDrawable(R.drawable.ic_thumb_up_pdark_24dp));
                    count++;
                    likeCount.setText(""+count);
                    prefManager.addLike(pao.uuid);
                }
                FirebaseDatabaseService.updateLikes(pao.uuid, count);
            }
        });

        final View dislikeContainer = fragmentView.findViewById(R.id.dislike_container);
        final ImageButton dislike = (ImageButton) fragmentView.findViewById(R.id.dislike);
        final TextView dislikeCount = (TextView) fragmentView.findViewById(R.id.dislike_count);
        likeCount.setText("" + pao.disLikes);
        dislikeContainer.setOnClickListener(new View.OnClickListener() {
            int count = pao.disLikes;
            @Override
            public void onClick(View view) {
                if (prefManager.hasDislike(pao.uuid)) {
                    dislike.setImageDrawable(getResources().getDrawable(R.drawable.ic_thumb_down_p_24dp));
                    count--;
                    dislikeCount.setText(""+count);
                    prefManager.removeDislike(pao.uuid);
                } else {
                    dislike.setImageDrawable(getResources().getDrawable(R.drawable.ic_thumb_down_pdark_24dp));
                    count++;
                    dislikeCount.setText(""+count);
                    prefManager.addDislike(pao.uuid);
                }
                FirebaseDatabaseService.updateLikes(pao.uuid, count);
            }
        });

        ImageButton share = (ImageButton) fragmentView.findViewById(R.id.share);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(paoActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(paoActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        // Show an explanation to the user *asynchronously* -- don't block this thread waiting for the user's response! After the user sees the explanation, try again to request the permission.
                    } else {
                        // No explanation needed, we can request the permission.
                        ActivityCompat.requestPermissions(paoActivity, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an app-defined int constant. The callback method gets the result of the request.
                    }
                } else {
                    new Share(paoActivity).getScreenShot().store("abc.jpg").shareImage("abc.jpg");
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new Share(paoActivity).getScreenShot().store("abc.jpg").shareImage("abc.jpg");
                } else {
                    // permission denied, boo! Disable the functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other permissions this app might request
        }
    }


}
