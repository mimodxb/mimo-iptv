package tv.mimo.app.mobile;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.*;
import tv.mimo.app.Channel;
import tv.mimo.app.Repository;
import tv.mimo.app.R;
import java.util.*;

public class FavoritesFragment extends MobileBaseFragment implements MobileMainActivity.Searchable {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private FavoritesAdapter adapter;

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_favorites;
    }

    @Override
    public void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = findView(view, R.id.favorites_recycler);
        emptyView = findView(view, R.id.favorites_empty);
        int spanCount = getResources().getBoolean(R.bool.is_tablet) ? 3 : 2;
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
        adapter = new FavoritesAdapter();
        recyclerView.setAdapter(adapter);

        loadFavorites();
    }

    private void loadFavorites() {
        Repository repo = new Repository(requireContext());
        Set<String> favoriteKeys = repo.favorites();
        Repository.Catalog catalog = repo.load(false);
        List<Channel> favoriteChannels = new ArrayList<>();
        for (Channel c : catalog.channels) {
            if (favoriteKeys.contains(c.key)) {
                favoriteChannels.add(c);
            }
        }
        adapter.setChannels(favoriteChannels);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSearch(String query) {
        adapter.filter(query);
        updateEmptyState();
    }

    static class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {
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
                holder.programme.setText(holder.itemView.getContext().getString(R.string.channel_available));
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
                // Favorites always show favorite icon
                favorite.setVisibility(View.VISIBLE);
            }
        }
    }
}