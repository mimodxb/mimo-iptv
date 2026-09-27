package tv.mimo.app.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.*;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;
import tv.mimo.app.Channel;
import tv.mimo.app.ChannelLogoLoader;
import tv.mimo.app.Repository;
import tv.mimo.app.R;
import java.util.*;

public class LiveTvFragment extends MobileBaseFragment implements MobileMainActivity.Searchable {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private LiveTvPagerAdapter pagerAdapter;
    private Repository repository;
    private ProgressBar loadingProgress;
    private TextView errorText;
    private FloatingActionButton refreshFab;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String[] CATEGORIES = {
        "Azerbaijan", "Russia", "Turkey", "Europe", "World", "All"
    };

    private static final int[] CATEGORY_TITLES = {
        R.string.home_category_azerbaijan,
        R.string.home_category_russia,
        R.string.home_category_turkey,
        R.string.home_category_europe,
        R.string.home_category_world,
        R.string.nav_live_tv
    };

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_live_tv;
    }

    @Override
    public void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new Repository(requireContext());

        tabLayout = findView(view, R.id.category_tabs);
        viewPager = findView(view, R.id.live_tv_pager);
        loadingProgress = findView(view, R.id.loading_progress);
        errorText = findView(view, R.id.error_text);
        refreshFab = findView(view, R.id.refresh_fab);

        pagerAdapter = new LiveTvPagerAdapter(getChildFragmentManager(), getLifecycle());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(1);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(CATEGORY_TITLES[position]);
        }).attach();

        refreshFab.setOnClickListener(v -> refreshCurrentCategory());
        if(getArguments()!=null){String category=getArguments().getString("category","Azerbaijan");for(int i=0;i<CATEGORIES.length;i++)if(CATEGORIES[i].equals(category))viewPager.setCurrentItem(i,false);}
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateRefreshFabVisibility();
            }
        });
    }

    private void updateRefreshFabVisibility() {
        Fragment fragment = pagerAdapter.getRegisteredFragment(viewPager.getCurrentItem());
        if (fragment instanceof ChannelGridFragment) {
            ChannelGridFragment gridFragment = (ChannelGridFragment) fragment;
            if (gridFragment.isEmpty()) {
                refreshFab.show();
            } else {
                refreshFab.hide();
            }
        }
    }

    private void refreshCurrentCategory() {
        Fragment fragment = pagerAdapter.getRegisteredFragment(viewPager.getCurrentItem());
        if (fragment instanceof ChannelGridFragment) {
            ((ChannelGridFragment) fragment).loadChannels(true);
        }
    }

    @Override
    public void onSearch(String query) {
        Fragment fragment = pagerAdapter.getRegisteredFragment(viewPager.getCurrentItem());
        if (fragment instanceof ChannelGridFragment) {
            ((ChannelGridFragment) fragment).filter(query);
        }
    }

    static class LiveTvPagerAdapter extends FragmentStateAdapter {
        private final Map<Integer, Fragment> fragmentMap = new HashMap<>();
        private final FragmentManager manager;

        LiveTvPagerAdapter(FragmentManager fm, Lifecycle lifecycle) {
            super(fm, lifecycle);
            manager=fm;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment fragment = ChannelGridFragment.newInstance(CATEGORIES[position]);
            fragmentMap.put(position, fragment);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return CATEGORIES.length;
        }

        Fragment getRegisteredFragment(int position) {
            Fragment restored=manager.findFragmentByTag("f"+position);
            return restored!=null?restored:fragmentMap.get(position);
        }
    }

    public static class ChannelGridFragment extends Fragment implements MobileMainActivity.Searchable {
        private static final String ARG_CATEGORY = "category";
        private String category;
        private RecyclerView recyclerView;
        private ChannelGridAdapter adapter;
        private List<Channel> channels = new ArrayList<>();
        private boolean isLoading = false;
        private final Handler mainHandler = new Handler(Looper.getMainLooper());

        public static ChannelGridFragment newInstance(String category) {
            ChannelGridFragment fragment = new ChannelGridFragment();
            Bundle args = new Bundle();
            args.putString(ARG_CATEGORY, category);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) {
                category = getArguments().getString(ARG_CATEGORY);
            }
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_channel_grid, container, false);
            recyclerView = view.findViewById(R.id.channel_grid);
            int spanCount = getResources().getBoolean(R.bool.is_tablet) ? 3 : 2;
            recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
            adapter = new ChannelGridAdapter();
            recyclerView.setAdapter(adapter);
            return view;
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            adapter.setFragment(this);
            loadChannels(false);
        }

        void loadChannels(boolean forceNetwork) {
            if (isLoading) return;
            isLoading = true;

            updateParentViews(true, null, null);

            MobileCatalog.load(requireContext(),forceNetwork,catalog->{
                if(getView()==null)return;
                isLoading=false;channels=new ArrayList<>();
                for(Channel c:catalog.channels)if("All".equals(category)||category.equals(c.category()))channels.add(c);
                adapter.setChannels(channels);
                Fragment parent=getParentFragment();if(parent!=null&&parent.getArguments()!=null)adapter.filter(parent.getArguments().getString("query",""));
                updateParentViews(false,catalog.notices.isEmpty()?(channels.isEmpty()?getString(R.string.live_tv_empty):null):String.join("\n",catalog.notices),channels.isEmpty());
            });
        }

        private void updateParentViews(boolean loading, String errorMsg, Boolean empty) {
            if (getActivity() == null) return;
            View rootView = getActivity().findViewById(android.R.id.content);
            if (rootView == null) return;

            ProgressBar progress = rootView.findViewById(R.id.loading_progress);
            TextView error = rootView.findViewById(R.id.error_text);
            FloatingActionButton refreshFab = rootView.findViewById(R.id.refresh_fab);

            if (progress != null) progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (error != null) {
                if (errorMsg != null) {
                    error.setText(errorMsg);
                    error.setVisibility(View.VISIBLE);
                } else {
                    error.setVisibility(View.GONE);
                }
            }
            if (refreshFab != null) {
                boolean showFab = (empty != null && empty) || (errorMsg != null);
                refreshFab.show();
            }
        }

        public boolean isEmpty() {
            return channels.isEmpty();
        }

        @Override
        public void onSearch(String query) {
            filter(query);
        }

        public void filter(String query) {
            if (adapter != null) {
                adapter.filter(query);
            }
        }
    }

    static class ChannelGridAdapter extends RecyclerView.Adapter<ChannelGridAdapter.ViewHolder> {
        private List<Channel> channels = new ArrayList<>();
        private List<Channel> filtered = new ArrayList<>();
        private ArrayList<String> channelKeys;
        private Fragment fragment;
        private static final int LOGO_SIZE_DP = 120;

        void setChannels(List<Channel> channels) {
            this.channels = channels;
            this.filtered = new ArrayList<>(channels);
            // Build channel keys list for navigation
            this.channelKeys = new ArrayList<>();
            for (Channel c : channels) {
                channelKeys.add(c.key);
            }
            notifyDataSetChanged();
        }

        void filter(String query) {
            filtered.clear();
            if (query == null || query.trim().isEmpty()) {
                filtered.addAll(channels);
            } else {
                String lower = query.toLowerCase(java.util.Locale.ROOT);
                for (Channel c : channels) {
                    if (c.name.toLowerCase(java.util.Locale.ROOT).contains(lower)) {
                        filtered.add(c);
                    }
                }
            }
            notifyDataSetChanged();
        }

        void setFragment(Fragment fragment) {
            this.fragment = fragment;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_channel_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Channel channel = filtered.get(position);
            holder.name.setText(channel.name);
            Repository favorites=new Repository(holder.itemView.getContext());
            holder.favorite.setImageResource(favorites.favorites().contains(channel.key)?R.drawable.ic_favorite_filled:R.drawable.ic_favorite);
            holder.favorite.setOnClickListener(v->{boolean added=favorites.toggleFavorite(channel.key);holder.favorite.setImageResource(added?R.drawable.ic_favorite_filled:R.drawable.ic_favorite);});
            holder.itemView.setOnLongClickListener(v->{holder.favorite.performClick();return true;});
            
            // Load channel logo
            ChannelLogoLoader.load(holder.logo, channel.logo, LOGO_SIZE_DP, LOGO_SIZE_DP, holder.itemView.getContext());

            // Programme info could be added from EPG data later
            if (channel.streams != null && !channel.streams.isEmpty()) {
                holder.programme.setText(holder.itemView.getContext().getString(R.string.channel_available));
                holder.programme.setVisibility(View.VISIBLE);
            } else {
                holder.programme.setVisibility(View.GONE);
            }

            // Click listener to launch playback
            holder.itemView.setOnClickListener(v -> {
                if (fragment != null && fragment.getActivity() != null) {
                    int filteredIndex = holder.getAdapterPosition();
                    if (filteredIndex >= 0 && filteredIndex < filtered.size()) {
                        Channel selectedChannel = filtered.get(filteredIndex);
                        int channelIdx = channelKeys.indexOf(selectedChannel.key);
                        launchPlayback(selectedChannel, channelIdx);
                    }
                }
            });
        }

        private void launchPlayback(Channel channel, int channelIdx) {
            Intent intent = new Intent(fragment.getActivity(), MobilePlaybackActivity.class);
            intent.putExtra("key", channel.key);
            intent.putExtra("name", channel.name);
            intent.putStringArrayListExtra("chan_keys", new ArrayList<>(channelKeys.subList(Math.max(0,channelIdx-100),Math.min(channelKeys.size(),channelIdx+101))));
            intent.putExtra("idx", Math.min(channelIdx,100));
            fragment.startActivity(intent);
        }

        @Override
        public int getItemCount() {
            return filtered.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView programme;
            final ImageView favorite;
            final ImageView logo;

            ViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.channel_name);
                programme = itemView.findViewById(R.id.channel_programme);
                favorite = itemView.findViewById(R.id.channel_favorite);
                logo = itemView.findViewById(R.id.channel_logo);
            }
        }
    }
}
