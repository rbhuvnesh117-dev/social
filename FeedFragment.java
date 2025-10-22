package finix.social.finixapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.core.widget.NestedScrollView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.balysv.materialripple.MaterialRippleLayout;
import  finix.social.finixapp.libs.circularImageView.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import finix.social.finixapp.adapter.AdvancedItemListAdapter;
import finix.social.finixapp.app.App;
import finix.social.finixapp.constants.Constants;
import finix.social.finixapp.model.Item;
import finix.social.finixapp.util.Api;
import finix.social.finixapp.util.CustomRequest;

public class FeedFragment extends Fragment implements Constants, SwipeRefreshLayout.OnRefreshListener {

    private static final String STATE_LIST = "State Adapter Data";

    private static final int PROFILE_NEW_POST = 4;

    private RecyclerView mRecyclerView;

    private BottomSheetBehavior mBehavior;
    private BottomSheetDialog mBottomSheetDialog;
    private View mBottomSheet;

    private ProgressBar mProgressBar;

    private SwipeRefreshLayout mItemsContainer;

    private ArrayList<Item> itemsList;
    private AdvancedItemListAdapter itemsAdapter;


    private int itemId = 0;
    private int arrayLength = 0;
    private Boolean loadingMore = false;
    private Boolean viewMore = false;
    private Boolean restore = false;
    private Boolean loaded = false;

    //

    private int pastVisiblesItems = 0, visibleItemCount = 0, totalItemCount = 0;

    public FeedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        if (savedInstanceState != null) {

            itemsList = savedInstanceState.getParcelableArrayList(STATE_LIST);
            itemsAdapter = new AdvancedItemListAdapter(getActivity(), itemsList);

            restore = savedInstanceState.getBoolean("restore");
            loaded = savedInstanceState.getBoolean("loaded");
            itemId = savedInstanceState.getInt("itemId");

        } else {

            itemsList = new ArrayList<>();
            itemsAdapter = new AdvancedItemListAdapter(getActivity(), itemsList);

            restore = false;
            loaded = false;
            itemId = 0;
        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_feed, container, false);

        mItemsContainer = (SwipeRefreshLayout) rootView.findViewById(R.id.container_items);
        mItemsContainer.setOnRefreshListener(this);

        //

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.GONE);

        // Prepare bottom sheet

        mBottomSheet = rootView.findViewById(R.id.bottom_sheet);
        mBehavior = BottomSheetBehavior.from(mBottomSheet);

        //

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);

        final GridLayoutManager mLayoutManager = new GridLayoutManager(getActivity(), 1);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemViewCacheSize(100);
        mRecyclerView.setDrawingCacheEnabled(true);
        mRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        mRecyclerView.setAdapter(itemsAdapter);
        itemsAdapter.attachToRecyclerView(mRecyclerView);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

                //check for scroll down

                if (dy > 0) {

                    visibleItemCount = mLayoutManager.getChildCount();
                    totalItemCount = mLayoutManager.getItemCount();
                    pastVisiblesItems = mLayoutManager.findFirstVisibleItemPosition();

                    if (!loadingMore) {

                        if ((visibleItemCount + pastVisiblesItems) >= totalItemCount && (viewMore) && !(mItemsContainer.isRefreshing())) {

                            loadingMore = true;

                            getItems();
                        }
                    }
                }
            }
        });



        itemsAdapter.setOnMoreButtonClickListener(new AdvancedItemListAdapter.OnItemMenuButtonClickListener() {

            @Override
            public void onItemClick(View v, Item obj, int actionId, int position) {

                switch (actionId) {

                    case ITEM_ACTION_REPOST: {

                        if (obj.getFromUserId() != App.getInstance().getId()) {

                            if (obj.getRePostFromUserId() != App.getInstance().getId()) {

                                repost(position);

                            } else {

                                Toast.makeText(getActivity(), getActivity().getString(R.string.msg_not_make_repost), Toast.LENGTH_SHORT).show();
                            }

                        } else {

                            Toast.makeText(getActivity(), getActivity().getString(R.string.msg_not_make_repost), Toast.LENGTH_SHORT).show();
                        }

                        break;
                    }

                    case ITEM_ACTIONS_MENU: {

                        showItemActionDialog(position);

                        break;
                    }

                    case ITEM_ACTIONS_SWITCH_MODE: {

                        itemId = 0;
                        getItems();

                        break;
                    }

                    case ITEM_ACTIONS_NEW_ITEM: {

                        Intent intent = new Intent(getActivity(), NewItemActivity.class);
                        startActivityForResult(intent, FEED_NEW_POST);

                        break;
                    }

                    case ITEM_ACTIONS_CLOSE_OTP_TOOLTIP: {

                        App.getInstance().getTooltipsSettings().setShowOtpTooltip(false);
                        App.getInstance().saveTooltipsSettings();

                        itemsList.remove(position);
                        itemsAdapter.notifyItemRemoved(position);

                        break;
                    }

                    case ITEM_ACTIONS_LINK_NUMBER: {

                        App.getInstance().getTooltipsSettings().setShowOtpTooltip(false);
                        App.getInstance().saveTooltipsSettings();

                        itemsList.remove(position);
                        itemsAdapter.notifyItemRemoved(position);

                        Intent intent = new Intent(getActivity(), OtpVerificationActivity.class);
                        startActivity(intent);

                        break;
                    }
                }
            }
        });

        if (!restore) {

            mProgressBar.setVisibility(View.VISIBLE);
            getItems();
        }

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        outState.putBoolean("restore", true);
        outState.putBoolean("loaded", loaded);
        outState.putInt("itemId", itemId);
        outState.putParcelableArrayList(STATE_LIST, itemsList);
    }

    @Override
    public void onRefresh() {

        if (App.getInstance().isConnected()) {

            itemId = 0;
            getItems();

        } else {

            mItemsContainer.setRefreshing(false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FEED_NEW_POST && resultCode == getActivity().RESULT_OK && null != data) {

            itemId = 0;
            getItems();

        } else if (requestCode == ITEM_EDIT && resultCode == getActivity().RESULT_OK) {

            int position = data.getIntExtra("position", 0);

            if (data.getExtras() != null) {

                Item item = (Item) data.getExtras().getParcelable("item");

                itemsList.set(position, item);
            }

            itemsAdapter.notifyDataSetChanged();

        } else if (requestCode == ITEM_REPOST && resultCode == getActivity().RESULT_OK) {

            int position = data.getIntExtra("position", 0);

            Item item = itemsList.get(position);

            item.setMyRePost(true);
            item.setRePostsCount(item.getRePostsCount() + 1);

            itemsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);
    }

    public void getItems() {

        mItemsContainer.setRefreshing(true);

        String url = METHOD_FEEDS_GET;

        if (App.getInstance().getFeedMode() != 1) {

            url = METHOD_STREAM_GET;
        }

        CustomRequest jsonReq = new CustomRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        if (!isAdded() || getActivity() == null) {

                            Log.e("ERROR", "FeedFragment Not Added to Activity");

                            return;
                        }

                        if (!loadingMore) {

                            itemsList.clear();
                            itemsAdapter.notifyDataSetChanged();

                            Item item = new Item();
                            item.setViewType(VIEW_TYPE_SWITCH_MODE);
                            itemsList.add(item);
                            itemsAdapter.notifyItemChanged(itemsList.size());

                            Item item2 = new Item();
                            item2.setViewType(VIEW_TYPE_NEW_ITEM);
                            itemsList.add(item2);
                            itemsAdapter.notifyItemChanged(itemsList.size());

                            if (App.getInstance().getOtpVerified() == 0) {

                                if (App.getInstance().getTooltipsSettings().isAllowShowOtpTooltip()) {

                                    Item item3 = new Item();
                                    item3.setViewType(VIEW_TYPE_OTP_TOOLTIP);
                                    itemsList.add(item3);
                                    itemsAdapter.notifyItemChanged(itemsList.size());
                                }
                            }
                        }

                        try {

                            arrayLength = 0;

                            if (!response.getBoolean("error")) {

                                itemId = response.getInt("itemId");

                                if (response.has("alerts")) {

                                    JSONArray alertsArray = response.getJSONArray("alerts");

                                    arrayLength = alertsArray.length();

                                    if (arrayLength > 0) {

                                        JSONObject itemObj = (JSONObject) alertsArray.get(0);

                                        Item item = new Item(itemObj);

                                        if (item.getId() != App.getInstance().getHiddenItemId()) {

                                            item.setViewType(VIEW_TYPE_DEFAULT);
                                            item.setAdminAccount(1);
                                            itemsList.add(item);
                                            itemsAdapter.notifyItemChanged(itemsList.size());
                                        }
                                    }

                                }

                                if (response.has("items")) {

                                    JSONArray itemsArray = response.getJSONArray("items");

                                    arrayLength = itemsArray.length();

                                    if (arrayLength > 0) {

                                        for (int i = 0; i < itemsArray.length(); i++) {

                                            JSONObject itemObj = (JSONObject) itemsArray.get(i);

                                            Item item = new Item(itemObj);
                                            item.setViewType(VIEW_TYPE_DEFAULT);

                                            if (!App.getInstance().getDatabaseManager().isExist(item.getFromUserId())) {

                                                itemsList.add(item);
                                                itemsAdapter.notifyItemChanged(itemsList.size());
                                            }

                                            // Ad after first item
                                            if (i == MY_AD_AFTER_ITEM_NUMBER && App.getInstance().getAdmob() == ADMOB_DISABLED && App.getInstance().isMobileAdsInitializeCalled.get()) {

                                                Item ad = new Item(itemObj);
                                                ad.setViewType(VIEW_TYPE_AD);

                                                itemsList.add(ad);
                                                itemsAdapter.notifyItemChanged(itemsList.size());
                                            }
                                        }
                                    }
                                }
                            }

                        } catch (JSONException e) {

                            e.printStackTrace();

                        } finally {

                            loadingComplete();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                if (!isAdded() || getActivity() == null) {

                    Log.e("ERROR", "FeedFragment Not Added to Activity");

                    return;
                }

                loadingComplete();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("accountId", Long.toString(App.getInstance().getId()));
                params.put("accessToken", App.getInstance().getAccessToken());
                params.put("itemId", Integer.toString(itemId));
                params.put("language", App.getInstance().getLanguage());

                return params;
            }
        };

        App.getInstance().addToRequestQueue(jsonReq);
    }

    public void loadingComplete() {

        mProgressBar.setVisibility(View.GONE);

        loaded = true;

        if (arrayLength == LIST_ITEMS) {

            viewMore = true;

        } else {

            viewMore = false;
        }

        //itemsAdapter.notifyDataSetChanged();

        if (itemsAdapter.getItemCount(VIEW_TYPE_DEFAULT) == 0) {

            if (FeedFragment.this.isVisible()) {

                Item item = new Item();
                item.setViewType(VIEW_TYPE_EMPTY_LIST);
                itemsList.add(item);
                itemsAdapter.notifyItemChanged(itemsList.size());
            }

        } else {

            if (itemsList.size() != 0) {

                itemsList.remove(itemsList.size() - 1);
                itemsAdapter.notifyItemRemoved(itemsList.size() - 1);
            }
        }

        loadingMore = false;
        mItemsContainer.setRefreshing(false);
    }

    // Item action


    private void showItemActionDialog(final int position) {

        if (mBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {

            mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        final View view = getLayoutInflater().inflate(R.layout.item_action_sheet_list, null);

        MaterialRippleLayout mLoginButton = (MaterialRippleLayout) view.findViewById(R.id.login_button);
        MaterialRippleLayout mSignupButton = (MaterialRippleLayout) view.findViewById(R.id.signup_button);

        mLoginButton.setVisibility(View.GONE);
        mSignupButton.setVisibility(View.GONE);

        MaterialRippleLayout mEditButton = (MaterialRippleLayout) view.findViewById(R.id.edit_button);
        MaterialRippleLayout mDeleteButton = (MaterialRippleLayout) view.findViewById(R.id.delete_button);
        MaterialRippleLayout mShareButton = (MaterialRippleLayout) view.findViewById(R.id.share_button);
        MaterialRippleLayout mRepostButton = (MaterialRippleLayout) view.findViewById(R.id.repost_button);
        MaterialRippleLayout mReportButton = (MaterialRippleLayout) view.findViewById(R.id.report_button);
        MaterialRippleLayout mOpenUrlButton = (MaterialRippleLayout) view.findViewById(R.id.open_url_button);
        MaterialRippleLayout mCopyUrlButton = (MaterialRippleLayout) view.findViewById(R.id.copy_url_button);
        MaterialRippleLayout mPinButton = (MaterialRippleLayout) view.findViewById(R.id.pin_button);
        MaterialRippleLayout mHideButton = (MaterialRippleLayout) view.findViewById(R.id.hide_button);
        MaterialRippleLayout mHideContentButton = (MaterialRippleLayout) view.findViewById(R.id.hide_content_button);
        TextView mHideContentButtonText = (TextView) view.findViewById(R.id.hide_content_button_text);

        mPinButton.setVisibility(View.GONE);
        mHideButton.setVisibility(View.GONE);
        mHideContentButton.setVisibility(View.GONE);

        if (!WEB_SITE_AVAILABLE) {

            mOpenUrlButton.setVisibility(View.GONE);
            mCopyUrlButton.setVisibility(View.GONE);
        }

        final Item item = itemsList.get(position);

        if (item.getFromUserId() == App.getInstance().getId()) {

            mEditButton.setVisibility(View.GONE);

            if (item.getPostType() == POST_TYPE_DEFAULT) {

                mEditButton.setVisibility(View.VISIBLE);
            }

            mDeleteButton.setVisibility(View.VISIBLE);

            mRepostButton.setVisibility(View.GONE);
            mReportButton.setVisibility(View.GONE);

        } else {

            mEditButton.setVisibility(View.GONE);
            mDeleteButton.setVisibility(View.GONE);

            mRepostButton.setVisibility(View.VISIBLE);
            mReportButton.setVisibility(View.VISIBLE);

            //
            mHideContentButton.setVisibility(View.VISIBLE);
            mHideContentButtonText.setText(String.format(getString(R.string.action_hide_content), item.getFromUserFullname()));
        }

        if (item.getAdminAccount() != 0) {

            mHideButton.setVisibility(View.VISIBLE);

            mEditButton.setVisibility(View.GONE);
            mDeleteButton.setVisibility(View.GONE);
            mShareButton.setVisibility(View.GONE);
            mRepostButton.setVisibility(View.GONE);
            mReportButton.setVisibility(View.GONE);
            mHideContentButton.setVisibility(View.GONE);
            mPinButton.setVisibility(View.GONE);
        }

        mHideButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                mBottomSheetDialog.dismiss();

                App.getInstance().setHiddenItemId(item.getId());
                App.getInstance().saveData();

                itemsList.remove(position);
                itemsAdapter.notifyItemRemoved(position);
            }
        });

        mHideContentButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                mBottomSheetDialog.dismiss();

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                alertDialog.setTitle(getText(R.string.label_block));

                alertDialog.setMessage(getText(R.string.label_hide_content));
                alertDialog.setCancelable(true);

                alertDialog.setNegativeButton(getText(R.string.action_no), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.cancel();
                    }
                });

                alertDialog.setPositiveButton(getText(R.string.action_yes), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                        if (!App.getInstance().getDatabaseManager().isExist(item.getFromUserId())) {

                            long unixTime = System.currentTimeMillis() / 1000L;

                            App.getInstance().getDatabaseManager().insert(item.getFromUserId(), unixTime);
                        }

                        Iterator<Item> iter = itemsList.iterator();

                        while (iter.hasNext()) {

                            Item it = iter.next();

                            if (it.getViewType() == VIEW_TYPE_DEFAULT && it.getFromUserId() == item.getFromUserId()) {

                                iter.remove();
                            }
                        }

                        itemsAdapter.notifyDataSetChanged();
                    }
                });

                alertDialog.show();
            }
        });

        mEditButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                mBottomSheetDialog.dismiss();

                Intent i = new Intent(getActivity(), NewItemActivity.class);
                i.putExtra("item", item);
                i.putExtra("position", position);
                startActivityForResult(i, ITEM_EDIT);
            }
        });

        mDeleteButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                mBottomSheetDialog.dismiss();

                delete(position);
            }
        });

        mShareButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                mBottomSheetDialog.dismiss();

                share(position);
            }
        });

        mRepostButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                mBottomSheetDialog.dismiss();

                repost(position);
            }
        });

        mReportButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                mBottomSheetDialog.dismiss();

                report(position);
            }
        });

        mCopyUrlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mBottomSheetDialog.dismiss();

                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(getActivity().CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("post url", item.getLink());
                clipboard.setPrimaryClip(clip);

                Toast.makeText(getActivity(), getText(R.string.msg_post_link_copied), Toast.LENGTH_SHORT).show();
            }
        });

        mOpenUrlButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                mBottomSheetDialog.dismiss();

                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(item.getLink()));
                startActivity(i);
            }
        });

        mBottomSheetDialog = new BottomSheetDialog(getActivity());

        mBottomSheetDialog.setContentView(view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            mBottomSheetDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        mBottomSheetDialog.show();

        doKeepDialog(mBottomSheetDialog);

        mBottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {

                mBottomSheetDialog = null;
            }
        });
    }

    // Prevent dialog dismiss when orientation changes
    private static void doKeepDialog(Dialog dialog){

        WindowManager.LayoutParams lp = new  WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(lp);
    }

    public void delete(final int position) {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(getText(R.string.label_delete));

        alertDialog.setMessage(getText(R.string.label_delete_msg));
        alertDialog.setCancelable(true);

        alertDialog.setNegativeButton(getText(R.string.action_no), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.cancel();
            }
        });

        alertDialog.setPositiveButton(getText(R.string.action_yes), new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                final Item item = itemsList.get(position);

                itemsList.remove(position);
                itemsAdapter.notifyDataSetChanged();

                if (itemsAdapter.getItemCount(VIEW_TYPE_DEFAULT) == 0) {

                    Item itm = new Item();
                    itm.setViewType(VIEW_TYPE_EMPTY_LIST);
                    itemsList.add(itm);
                    itemsAdapter.notifyItemChanged(itemsList.size());
                }

                if (App.getInstance().isConnected()) {

                    Api api = new Api(getActivity());

                    api.postDelete(item.getId(), 1);

                } else {

                    Toast.makeText(getActivity(), getText(R.string.msg_network_error), Toast.LENGTH_SHORT).show();
                }
            }
        });

        alertDialog.show();
    }

    public void share(final int position) {

        final Item item = itemsList.get(position);

        Api api = new Api(getActivity());
        api.postShare(item);
    }

    public void repost(final int position) {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(getText(R.string.label_post_share));

        alertDialog.setMessage(getText(R.string.label_post_share_desc));
        alertDialog.setCancelable(true);

        alertDialog.setNegativeButton(getText(R.string.action_no), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.cancel();
            }
        });

        alertDialog.setPositiveButton(getText(R.string.action_yes), new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                Item item = itemsList.get(position);

                Intent i = new Intent(getActivity(), NewItemActivity.class);
                i.putExtra("position", position);
                i.putExtra("repost", item);
                startActivityForResult(i, ITEM_REPOST);
            }
        });

        alertDialog.show();
    }

    public void report(final int position) {

        String[] profile_report_categories = new String[] {

                getText(R.string.label_profile_report_0).toString(),
                getText(R.string.label_profile_report_1).toString(),
                getText(R.string.label_profile_report_2).toString(),
                getText(R.string.label_profile_report_3).toString(),

        };

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(getText(R.string.label_post_report_title));

        alertDialog.setSingleChoiceItems(profile_report_categories, 0, null);
        alertDialog.setCancelable(true);

        alertDialog.setNegativeButton(getText(R.string.action_cancel), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.cancel();
            }
        });

        alertDialog.setPositiveButton(getText(R.string.action_ok), new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                AlertDialog alert = (AlertDialog) dialog;
                int reason = alert.getListView().getCheckedItemPosition();

                final Item item = itemsList.get(position);

                Api api = new Api(getActivity());

                api.newReport(item.getId(), REPORT_TYPE_ITEM, reason);

                Toast.makeText(getActivity(), getActivity().getString(R.string.label_post_reported), Toast.LENGTH_SHORT).show();
            }
        });

        alertDialog.show();
    }

    //

    private void showPostDialog() {

        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_post);
        dialog.setCancelable(true);

        NestedScrollView mDlgNestedView = (NestedScrollView) dialog.findViewById(R.id.nested_view);
        RecyclerView mDlgRecyclerView = (RecyclerView) dialog.findViewById(R.id.recycler_view);

        final GridLayoutManager mLayoutManager = new GridLayoutManager(getActivity(), 1);
        mDlgRecyclerView.setLayoutManager(mLayoutManager);

        mDlgRecyclerView.setAdapter(itemsAdapter);

        mDlgRecyclerView.setNestedScrollingEnabled(true);

        final AppCompatButton bt_submit = (AppCompatButton) dialog.findViewById(R.id.bt_submit);
        ((EditText) dialog.findViewById(R.id.et_post)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                bt_submit.setEnabled(!s.toString().trim().isEmpty());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        bt_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Toast.makeText(getActivity(), "Post Submitted", Toast.LENGTH_SHORT).show();
            }
        });

        ((ImageButton) dialog.findViewById(R.id.bt_photo)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Post Photo Clicked", Toast.LENGTH_SHORT).show();
            }
        });

        ((ImageButton) dialog.findViewById(R.id.bt_link)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Post Link Clicked", Toast.LENGTH_SHORT).show();
            }
        });

        ((ImageButton) dialog.findViewById(R.id.bt_file)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Post File Clicked", Toast.LENGTH_SHORT).show();
            }
        });

        ((ImageButton) dialog.findViewById(R.id.bt_setting)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Post Setting Clicked", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();

        doKeepDialog(dialog);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }



    @Override
    public void onResume() {
        super.onResume();
        if (itemsAdapter != null) itemsAdapter.resumeCurrentIfVisible();
    }





    @Override
    public void onPause() {
        super.onPause();
        if (itemsAdapter != null) {
            itemsAdapter.pauseCurrent(); // pause any active playback
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (itemsAdapter != null) {
            itemsAdapter.releaseCurrent();        // release per-item player
            itemsAdapter.releaseSharedPlayer();   // release shared auto-play player
        }
    }

}