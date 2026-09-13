package tv.mimo.app.mobile;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.*;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;
import java.util.*;

public class LiveTvFragment extends MobileBaseFragment implements MobileMainActivity.Searchable {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private LiveTvPagerAdapter pagerAdapter;

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
    protected void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = findView(view, R.id.category_tabs);
        viewPager = findView(view, R.id.live_tv_pager);

        pagerAdapter = new LiveTvPagerAdapter();
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(CATEGORIES.length);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(CATEGORY_TITLES[position]);
        }).attach();
    }

    @Override
    public void onSearch(String query) {
        // Filter current category's channels
        Fragment fragment = pagerAdapter.getItem(viewPager.getCurrentItem());
        if (fragment instanceof ChannelGridFragment) {
            ((ChannelGridFragment) fragment).filter(query);
        }
    }

    static class LiveTvPagerAdapter extends androidx.fragment.app.FragmentStateAdapter {
        LiveTvPagerAdapter() {
            super(requireActivity().getSupportFragmentManager(), androidx.lifecycle.Lifecycle.getDefault());
        }

        LiveTvPagerAdapter(androidx.fragment.app.FragmentManager fm, androidx.lifecycle.Lifecycle lifecycle) {
            super(fm, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return ChannelGridFragment.newInstance(CATEGORIES[position]);
        }

        @Override
        public int getItemCount() {
            return CATEGORIES.length;
        }

        Fragment getItem(int position) {
            return (Fragment) instantiateItem(viewPager, position);
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
            recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            adapter = new ChannelGridAdapter();
            recyclerView.setAdapter(adapter);
            loadChannels();
            return view;
        }

        private void loadChannels() {
            // TODO: Load from Repository - placeholder for now
            channels = createMockChannels();
            adapter.setChannels(channels);
        }

        private List<Channel> createMockChannels() {
            List<Channel> list = new ArrayList<>();
            // Mock data - in real implementation, fetch from Repository
            for (int i = 0; i < 12; i++) {
                list.add(new Channel("Channel " + (i + 1), category, ""));
            }
            return list;
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

    static class Channel {
        final String name;
        final String category;
        final String programme;

        Channel(String name, String category, String programme) {
            this.name = name;
            this.category = category;
            this.programme = programme;
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
            if (channel.programme != null && !channel.programme.isEmpty()) {
                holder.programme.setText(channel.programme);
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