package com.example.localbase.adapter

import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.localbase.R
import com.example.localbase.clickEvent.faceDetails
import com.example.localbase.database.Student
import com.google.android.material.imageview.ShapeableImageView
import java.io.File

class FaceAdapter(
    private val faceClick: faceDetails,
    val data : List<Student>
) : RecyclerView.Adapter<FaceAdapter.viewHolder>() {

    class viewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(R.id.userName)
        val time = view.findViewById<TextView>(R.id.userInfo)
        val date = view.findViewById<TextView>(R.id.dateinfo)
        val faceId = view.findViewById<TextView>(R.id.uniqueId)
        val faceValue = view.findViewById<ShapeableImageView>(R.id.userAvatar)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): viewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_face, null)
        return viewHolder(view)
    }

    override fun onBindViewHolder(
        holder: viewHolder,
        position: Int
    ) {
        val faceData = data[position]
        holder.date.text = "Date: "+faceData.date
        holder.time.text = "Time: "+faceData.time
        holder.name.text = faceData.name
        holder.faceId.text = faceData.id.toString()

        val getRn = holder.itemView.context.getDrawable(R.drawable.birds)

        Glide.with(holder.itemView.context)
            .load(getRn)
            .centerCrop()
            .fitCenter()
            .circleCrop()
            .placeholder(getRn)
            .into(holder.faceValue)

        holder.itemView.setOnClickListener {
            faceClick.edit(faceData)
        }
    }

    override fun getItemCount(): Int = data.size
}