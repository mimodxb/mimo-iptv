package tv.mimo.app.mobile;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.*;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;
import tv.mimo.app.Channel;
import tv.mimo.app.Repository;
import tv.mimo.app.Source;
import tv.mimo.app.R;
import java.util.*;

public class LiveTvFragment extends MobileBaseFragment implements MobileMainActivity.Searchable {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private LiveTvPagerAdapter pagerAdapter;
    private Repository repository;

    private static final String[] CATEGORIES = {
        "Azerbaijan", "Russia", "Turkey", "Europe", "World"
    };

    private static final int[] CATEGORY_TITLES = {
        R.string.home_category_azerbaijan,
        R.string.home_category_russia,
        R.string.home_category_turkey,
        R.string.home_category_europe,
        R.string.home_category_world
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

        pagerAdapter = new LiveTvPagerAdapter(getChildFragmentManager(), getLifecycle());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(CATEGORIES.length);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(CATEGORY_TITLES[position]);
        }).attach();
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

        LiveTvPagerAdapter(FragmentManager fm, Lifecycle lifecycle) {
            super(fm, lifecycle);
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
            return fragmentMap.get(position);
        }
    }

    public static class ChannelGridFragment extends Fragment implements MobileMainActivity.Searchable {
        private static final String ARG_CATEGORY = "category";
        private String category;
        private RecyclerView recyclerView;
        private ChannelGridAdapter adapter;
        private List<Channel> channels = new ArrayList<>();

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
            loadChannels();
            return view;
        }

        private void loadChannels() {
            // Load channels from Repository for this category
            Repository repo = new Repository(requireContext());
            Repository.Catalog catalog = repo.load(false); // use cached data initially
            List<Channel> categoryChannels = new ArrayList<>();
            for (Channel c : catalog.channels) {
                if (category.equals(c.category())) {
                    categoryChannels.add(c);
                }
            }
            channels = categoryChannels;
            adapter.setChannels(channels);
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

        void setChannels(List<Channel> channels) {
            this.channels = channels;
            this.filtered = new ArrayList<>(channels);
            notifyDataSetChanged();
        }

        void filter(String query) {
            filtered.clear();
            if (query == null || query.trim().isEmpty()) {
                filtered.addAll(channels);
            } else {
                String lower = query.toLowerCase();
                for (Channel c : channels) {
                    if (c.name.toLowerCase().contains(lower)) {
                        filtered.add(c);
                    }
                }
            }
            notifyDataSetChanged();
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
            // Programme info could be added from EPG data later
            if (channel.streams != null && !channel.streams.isEmpty()) {
                holder.programme.setText("Available");
                holder.programme.setVisibility(View.VISIBLE);
            } else {
                holder.programme.setVisibility(View.GONE);
            }
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