package tv.mimo.app.mobile;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.*;

public class FavoritesFragment extends MobileBaseFragment implements MobileMainActivity.Searchable {

    private RecyclerView recyclerView;
    private FavoritesAdapter adapter;

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_favorites;
    }

    @Override
    protected void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = findView(view, R.id.favorites_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new FavoritesAdapter();
        recyclerView.setAdapter(adapter);

        // Load favorites from Repository - placeholder for now
        loadFavorites();
    }

    private void loadFavorites() {
        // TODO: Load from Repository.favorites()
        // For now, show empty state
        adapter.setChannels(new java.util.ArrayList<>());
    }

    @Override
    public void onSearch(String query) {
        adapter.filter(query);
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
                // Favorites always show favorite icon
                favorite.setVisibility(View.VISIBLE);
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
}