package com.example.localbase.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.localbase.R
import com.example.localbase.clickEvent.faceDetails
import com.example.localbase.database.Student
import com.example.localbase.databinding.ItemUserFaceBinding
import com.example.localbase.helper.RotateTransformation
import com.example.localbase.helper.ToolBar
import java.io.File

class FaceAdapter(
    private val faceClick: faceDetails,
    private val data: List<Student>,
    private var toolBar: ToolBar
) : RecyclerView.Adapter<FaceAdapter.FaceViewHolder>() {

    inner class FaceViewHolder(val binding: ItemUserFaceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: Student) = with(binding) {

            dateinfo.text = "Date: " + data.date
            userInfo.text = "Time: " + data.time
            userName.text = data.name
            uniqueId.text = data.id.toString()

            val getRn = root.context.getDrawable(R.drawable.birds)

            data.imagePath.let {
                Glide.with(root.context)
                    .load(File(it.toString()))
                    .transform(RotateTransformation(0f))
                    .circleCrop()
                    .placeholder(getRn)
                    .into(userAvatar)
            }

            editButton.setOnClickListener {
                faceClick.edit(data)
            }

            deleteButton.setOnClickListener {
                faceClick.delete(data)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FaceViewHolder {
        val binding = ItemUserFaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FaceViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: FaceViewHolder,
        position: Int
    ) {
        val faceData = data[position]
        holder.bind(faceData)
    }

    override fun getItemCount(): Int = data.size
}