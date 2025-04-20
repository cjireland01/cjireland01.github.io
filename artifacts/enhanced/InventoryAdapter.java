package com.example.projectthree;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * InventoryAdapter binds a list of inventory items to a RecyclerView.
 * Displays item details such as name, quantity, date, and location.
 */
public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private final List<Item> itemList;

    /**
     * Constructs the InventoryAdapter with a list of items to display.
     *
     * @param itemList - List of inventory items
     */
    public InventoryAdapter(List<Item> itemList) {
        this.itemList = itemList;
    }

    /**
     * ViewHolder for inventory items in the RecyclerView.
     * Holds references to the TextViews for item details.
     */
    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        TextView itemNameText, quantityText, dateText, locationText;

        /**
         * Constructs a new InventoryViewHolder and binds view elements.
         *
         * @param itemView - The root view for an individual inventory item row
         */
        public InventoryViewHolder(View itemView) {
            super(itemView);
            itemNameText = itemView.findViewById(R.id.itemNameText);
            quantityText = itemView.findViewById(R.id.quantityText);
            dateText = itemView.findViewById(R.id.dateText);
            locationText = itemView.findViewById(R.id.locationText);
        }
    }

    /**
     * Inflates the layout for each inventory row in the RecyclerView.
     *
     * @param parent - The parent view group
     * @param viewType - The type of view (unused in this context)
     * @return A new instance of InventoryViewHolder
     */
    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory_row, parent, false);
        return new InventoryViewHolder(view);
    }

    /**
     * Binds data from the inventory item to the view holder at the given position.
     *
     * @param holder - The view holder for the current row
     * @param position - The index of the current item in the list
     */
    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        Item item = itemList.get(position);
        holder.itemNameText.setText(item.getItemName());
        holder.quantityText.setText("Qty: " + item.getQuantity());
        holder.dateText.setText(item.getDate());
        holder.locationText.setText(item.getLocationId());
    }

    /**
     * Returns the number of items in the adapter.
     *
     * @return The size of the item list
     */
    @Override
    public int getItemCount() {
        return itemList.size();
    }
}
