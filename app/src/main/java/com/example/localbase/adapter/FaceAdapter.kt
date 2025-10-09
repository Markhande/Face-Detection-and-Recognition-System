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
import com.example.localbase.helper.RotateTransformation
import com.example.localbase.helper.ToolBar
import com.google.android.material.imageview.ShapeableImageView
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

class FaceAdapter(
    private val faceClick: faceDetails,
    private val data: List<Student>,
    private var toolBar: ToolBar
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
        holder.date.text = "Date: " + faceData.date
        holder.time.text = "Time: " + faceData.time
        holder.name.text = faceData.name
        holder.faceId.text = faceData.id.toString()

        val getRn = holder.itemView.context.getDrawable(R.drawable.birds)

        faceData.imagePath.let {
            Glide.with(holder.itemView.context)
                .load(File(it))
                .transform(RotateTransformation(0f))
                .placeholder(getRn)
                .into(holder.faceValue)
        }


        holder.itemView.setOnClickListener {
            faceClick.edit(faceData)
        }
    }

    override fun getItemCount(): Int = data.size
}