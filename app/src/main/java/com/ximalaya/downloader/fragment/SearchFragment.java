/**
 * SearchFragment.java
 * com.ximalaya.downloader
 *
 * Function�� TODO 
 *
 *   ver     date      		author
 * --------------------------------------------
 *   		 2014-2-18 		chadwii
 *
 * Copyright (c) 2014, TNT All Rights Reserved.
*/

package com.ximalaya.downloader.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.loopj.android.http.RequestParams;
import com.ximalaya.downloader.AppConstant;
import com.ximalaya.downloader.modle.SoundInfo;
import com.ximalaya.downloader.net.HttpCallback;
import com.ximalaya.downloader.util.HttpUtil;
import com.ximalaya.downloader.MainTabActivity;
import com.ximalaya.downloader.R;
import com.ximalaya.downloader.modle.SearchSound;
import com.ximalaya.downloader.download.DownloadManager;
import com.ximalaya.downloader.view.PullToRefreshListView;
import com.ximalaya.downloader.view.XMImageView;

import org.apache.http.Header;

import java.util.ArrayList;

/**
 * ClassName:SearchFragment
 * Function: TODO ADD FUNCTION
 * Reason:	 TODO ADD REASON
 *
 * @author   chadwii
 * @version  
 * @since    Ver 1.1
 * @Date	 2014-2-18 4:28:40
 *
 */
public class SearchFragment extends Fragment 
{
	
	private View mContent;
	private PullToRefreshListView mListView;
	private EditText mEditText;
	private Button mButton;
	private Activity mContext;
	private String mKeyWord;
	private int mPageId;
	private int mTotalCount;
	private int mPageSize = 15;
	private ArrayList<SearchSound> mSearchResult =
			new ArrayList<SearchSound>();
	private SearchAdapter mAdapter;
	private boolean isFirst = true;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) 
	{
		
		super.onActivityCreated(savedInstanceState);
		mContext = getActivity();
		initUI();
	}
	
	private void initUI()
	{
		mEditText = (EditText) mContent.findViewById(R.id.search_et);
		mButton = (Button) mContent.findViewById(R.id.search_btn);
		mListView = (PullToRefreshListView) mContent.
				findViewById(R.id.search_result);
		mAdapter = new SearchAdapter();
		mListView.setAdapter(mAdapter);
		mListView.setOnScrollListener(new OnScrollListener()
		{
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState)
			{
				if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
					int count = view.getCount();
					count = count > 5 ? count - 5 : count - 1;// 提前5个item开始判断是否加载
					// 停止滚动，判断是否加载下一页数据
					if ((view.getLastVisiblePosition() > count && moreDataAvailable())) {

						mPageId++;
						searchSound(mKeyWord);
					}
				}
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount)
			{
				
			}
		});

		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				SearchSound sound = (SearchSound) mAdapter.getItem(position);
				if (sound == null) {
					return;
				}
				playSoundOnline(sound);
			}
		});

		mButton.setOnClickListener(new OnClickListener() 
		{
			
			@Override
			public void onClick(View v) 
			{
				mKeyWord = mEditText.getEditableText().toString();
				mPageId = 1;
				searchSound(mKeyWord);
			}
		});
	}

	private void playSoundOnline(SearchSound sound) {
		try {
			String audio = sound.play_path_aac_v224;
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.parse(audio), "audio/*");
			startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean moreDataAvailable()
	{
		if (mPageId * mPageSize < mTotalCount)
		{
			return true;
		}
		return false;
	}
	
	private void searchSound(final String keyWord)
	{
		if (keyWord == null || "".equals(keyWord))
		{
			return;
		}
		String url = AppConstant.APP_HOST + "s/mobile/search";
		RequestParams param = new RequestParams();
		param.add("condition", keyWord);
		param.add("scope", "voice");
		param.add("page", "" + mPageId);
		param.add("per_page", "" + mPageSize);
		HttpUtil.getInstance().httpGet(url, param, new HttpCallback()
		{

			ProgressDialog pd;
			@Override
			public void onFailure(int arg0, Header[] arg1, byte[] arg2,
					Throwable arg3) {
				super.onFailure(arg0, arg1, arg2, arg3);
				if (pd != null)
				{
					pd.dismiss();
				}
			}

			@Override
			public void onStart() {
				
				super.onStart();
				if (pd == null)
				{
					pd = new ProgressDialog(mContext);
				}
				else
				{
					pd.dismiss();
				}
				pd.setTitle("搜索\"" + keyWord + "\"");
				pd.setMessage("正在搜索,请稍候...");
				pd.show();
				
			}

			
			
			@Override
			public void onSuccess(int arg0, Header[] arg1, byte[] arg2) {
				
				// TODO Auto-generated method stub
				super.onSuccess(arg0, arg1, arg2);
				if (pd != null)
				{
					pd.dismiss();
				}
				if (arg2 == null || arg2.length == 0)
				{
					return;
				}
				String content = new String(arg2);
				JSONObject json = JSON.parseObject(content);
				if (json == null || 
						json.getJSONObject("responseHeader") == null || 
						json.getJSONObject("responseHeader").
						getInteger("status") != 0)
				{
					return;
				}
				if (json.getJSONObject("response") == null || 
						json.getJSONObject("response").
						getString("docs") == null)
				{
					return;
				}
				mTotalCount = json.getJSONObject("response").getInteger("numFound");
				if (mPageId == 1)
				{
					mSearchResult.clear();
				}
				mSearchResult.addAll(JSON.parseArray(json.
						getJSONObject("response").getString("docs"), 
						SearchSound.class));
				mAdapter.notifyDataSetChanged();
			}
			
		});
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) 
	{
		mContent = inflater.inflate(R.layout.search, null);
		return mContent;
	}
	
	@Override
	public void onDestroyView() 
	{
		
		super.onDestroyView();
	}
	
	public void onDestroy() 
	{
		super.onDestroy();
	}
	
	class SearchAdapter extends BaseAdapter
	{

		public SearchAdapter() 
		{

		}
		
		@Override
		public int getCount() 
		{
			return mSearchResult == null ? 0 : mSearchResult.size();
		}

		@Override
		public Object getItem(int position) 
		{
			return getCount() > 0 ? mSearchResult.get(position) : null;
		}

		@Override
		public long getItemId(int arg0) 
		{
			
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) 
		{
			ViewHolder holder = null;
			SearchSound sound = mSearchResult.get(position);
			if (convertView == null)
			{
				holder = new ViewHolder();
				convertView = View.inflate(mContext, R.layout.search_result_item, null);
				holder.icon = (XMImageView) convertView.findViewById(R.id.sound_icon);
				holder.title = (TextView) convertView.findViewById(R.id.sound_title);
				holder.progressBar = (ProgressBar) convertView.findViewById(R.id.download_progress);
				holder.download = (TextView) convertView.findViewById(R.id.sound_download);
				convertView.setTag(holder);
				holder.download.setOnClickListener(new OnClickListener() 
				{
					
					@Override
					public void onClick(View view) 
					{
						SearchSound ss = (SearchSound) view.getTag();
						if (ss == null)
						{
							return;
						}
						Toast.makeText(mContext, "开始下载\"" + ss.title + "\"", Toast.LENGTH_SHORT).show();
						DownloadManager.getInstance().downloadSound(ss, false);
						if (isFirst)
						{
							((MainTabActivity) mContext)
									.setCurrentTab(MainTabActivity.TAB_B);
							isFirst = false;
						}
					}
				});
			}
			else
			{
				holder = (ViewHolder) convertView.getTag();
			}
			holder.download.setTag(sound);
			if (!TextUtils.isEmpty(sound.cover_path)) {
				holder.icon.setImageURI(Uri.parse(sound.cover_path));
			}
			holder.title.setText(sound.title);
			return convertView;
		}
		
	}
	
	class ViewHolder 
	{
		XMImageView icon;
		TextView title;
		ProgressBar progressBar;
		TextView download;
	}
}

