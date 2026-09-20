package tv.mimo.app.mobile;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.*;
import tv.mimo.app.Channel;
import tv.mimo.app.ChannelLogoLoader;
import tv.mimo.app.R;
import tv.mimo.app.Repository;
import java.util.*;

public class HomeFragment extends MobileBaseFragment implements MobileMainActivity.Searchable {

    private RecyclerView recyclerView;
    private HomeAdapter adapter;
    private Repository repository;

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_home;
    }

    @Override
    public void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = findView(view, R.id.home_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HomeAdapter();
        recyclerView.setAdapter(adapter);

        repository = new Repository(requireContext());
        loadHomeData();
    }

    private void loadHomeData() {
        Repository.Catalog catalog = repository.load(false);
        List<String> recentKeys = repository.recentlyWatched();
        Set<String> favoriteKeys = repository.favorites();

        List<HomeSection> sections = new ArrayList<>();

        // Branding header
        sections.add(new HomeSection(HomeSection.TYPE_BRANDING, null));

        // Recently Watched
        List<Channel> recentChannels = new ArrayList<>();
        for (String key : recentKeys) {
            for (Channel c : catalog.channels) {
                if (key.equals(c.key)) {
                    recentChannels.add(c);
                    break;
                }
            }
        }
        sections.add(new HomeSection(HomeSection.TYPE_RECENTLY_WATCHED, recentChannels));

        // Favorites
        List<Channel> favoriteChannels = new ArrayList<>();
        for (String key : favoriteKeys) {
            for (Channel c : catalog.channels) {
                if (key.equals(c.key)) {
                    favoriteChannels.add(c);
                    break;
                }
            }
        }
        sections.add(new HomeSection(HomeSection.TYPE_FAVORITES, favoriteChannels));

        // Categories
        List<CategoryItem> categories = new ArrayList<>();
        categories.add(new CategoryItem(R.string.home_category_azerbaijan, "Azerbaijan"));
        categories.add(new CategoryItem(R.string.home_category_russia, "Russia"));
        categories.add(new CategoryItem(R.string.home_category_turkey, "Turkey"));
        categories.add(new CategoryItem(R.string.home_category_europe, "Europe"));
        categories.add(new CategoryItem(R.string.home_category_world, "World"));
        sections.add(new HomeSection(HomeSection.TYPE_CATEGORIES, categories));

        adapter.setData(sections);
    }

    @Override
    public void onSearch(String query) {
        // Navigate to Live TV with search or show search results
    }

    public void refreshData() {
        loadHomeData();
    }

    static class HomeSection {
        static final int TYPE_BRANDING = 0;
        static final int TYPE_RECENTLY_WATCHED = 1;
        static final int TYPE_FAVORITES = 2;
        static final int TYPE_CATEGORIES = 3;

        final int type;
        final Object data;

        HomeSection(int type, Object data) {
            this.type = type;
            this.data = data;
        }
    }

    static class CategoryItem {
        final int nameRes;
        final String categoryKey;

        CategoryItem(int nameRes, String categoryKey) {
            this.nameRes = nameRes;
            this.categoryKey = categoryKey;
        }
    }

    static class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_BRANDING = 0;
        private static final int VIEW_SECTION = 1;
        private static final int VIEW_CATEGORY_GRID = 2;

        private List<HomeSection> sections = new ArrayList<>();

        void setData(List<HomeSection> sections) {
            this.sections = sections;
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            HomeSection section = sections.get(position);
            if (section.type == HomeSection.TYPE_BRANDING) return VIEW_BRANDING;
            if (section.type == HomeSection.TYPE_CATEGORIES) return VIEW_CATEGORY_GRID;
            return VIEW_SECTION;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_BRANDING) {
                View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_home_branding, parent, false);
                return new BrandingViewHolder(view);
            } else if (viewType == VIEW_CATEGORY_GRID) {
                View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_grid, parent, false);
                return new CategoryGridViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_home_section, parent, false);
                return new SectionViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            HomeSection section = sections.get(position);
            if (holder instanceof BrandingViewHolder) {
                ((BrandingViewHolder) holder).bind();
            } else if (holder instanceof SectionViewHolder) {
                ((SectionViewHolder) holder).bind(section);
            } else if (holder instanceof CategoryGridViewHolder) {
                ((CategoryGridViewHolder) holder).bind((List<CategoryItem>) section.data);
            }
        }

        @Override
        public int getItemCount() {
            return sections.size();
        }

        static class BrandingViewHolder extends RecyclerView.ViewHolder {
            BrandingViewHolder(View itemView) { super(itemView); }
            void bind() { /* branding is static */ }
        }

        static class SectionViewHolder extends RecyclerView.ViewHolder {
            private final TextView title;
            private final RecyclerView recycler;
            private final TextView emptyText;

            SectionViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.section_title);
                recycler = itemView.findViewById(R.id.section_recycler);
                emptyText = itemView.findViewById(R.id.section_empty);
            }

            void bind(HomeSection section) {
                String titleText = "";
                List<?> items = new ArrayList<>();
                if (section.type == HomeSection.TYPE_RECENTLY_WATCHED) {
                    titleText = itemView.getContext().getString(R.string.home_recently_watched);
                } else if (section.type == HomeSection.TYPE_FAVORITES) {
                    titleText = itemView.getContext().getString(R.string.home_favorites);
                }
                title.setText(titleText);
                items = (List<?>) section.data;

                if (items.isEmpty()) {
                    recycler.setVisibility(View.GONE);
                    emptyText.setVisibility(View.VISIBLE);
                } else {
                    recycler.setVisibility(View.VISIBLE);
                    emptyText.setVisibility(View.GONE);
                    recycler.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayout.HORIZONTAL, false));
                    recycler.setAdapter(new ChannelHorizontalAdapter((List<Channel>) items));
                }
            }
        }

        static class CategoryGridViewHolder extends RecyclerView.ViewHolder {
            private final RecyclerView recycler;

            CategoryGridViewHolder(View itemView) {
                super(itemView);
                recycler = itemView.findViewById(R.id.category_grid);
            }

            void bind(List<CategoryItem> categories) {
                recycler.setLayoutManager(new GridLayoutManager(itemView.getContext(), 3));
                recycler.setAdapter(new CategoryAdapter(categories));
            }
        }
    }

    static class ChannelHorizontalAdapter extends RecyclerView.Adapter<ChannelHorizontalAdapter.ViewHolder> {
        private final List<Channel> items;
        private static final int LOGO_SIZE_DP = 90;
        ChannelHorizontalAdapter(List<Channel> items) { this.items = items; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_channel_card_small, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Channel channel = items.get(position);
            holder.name.setText(channel.name);
            if (channel.logo != null && !channel.logo.isEmpty()) {
                ChannelLogoLoader.load(holder.logo, channel.logo, LOGO_SIZE_DP, LOGO_SIZE_DP, holder.itemView.getContext());
            } else {
                holder.logo.setImageBitmap(ChannelLogoLoader.placeholder(holder.itemView.getContext(), LOGO_SIZE_DP, LOGO_SIZE_DP));
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final ImageView logo;
            ViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.channel_name);
                logo = itemView.findViewById(R.id.channel_logo);
            }
        }
    }

    static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private final List<CategoryItem> categories;
        CategoryAdapter(List<CategoryItem> categories) { this.categories = categories; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CategoryItem item = categories.get(position);
            holder.name.setText(item.nameRes);
        }

        @Override
        public int getItemCount() { return categories.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView name;
            ViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.category_name);
            }
        }
    }
}