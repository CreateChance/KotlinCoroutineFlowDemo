package com.example.kotlincoroutineflowdemo.fragments

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.kotlincoroutineflowdemo.databinding.FragmentRoomBinding
import com.example.kotlincoroutineflowdemo.databinding.RoomItemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class RoomFragment : Fragment() {
    private var viewBinding: FragmentRoomBinding? = null

    private val viewModel: UserViewModel by viewModels<UserViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        viewBinding = FragmentRoomBinding.inflate(inflater, container, false)

        viewBinding?.apply {
            btnAdd.setOnClickListener {
                viewModel.insert(
                    etId.text.toString(),
                    etFirstName.text.toString(),
                    etLastName.text.toString()
                )
            }

            val adapter = RoomListAdapter()
            rcvUserList.adapter = adapter
            rcvUserList.layoutManager = LinearLayoutManager(context)

            lifecycleScope.launch {
                viewModel.getAll().collect {
                    adapter.updateUsers(it)
                }
            }
        }

        return viewBinding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }
}

class UserViewModel(application: Application) : AndroidViewModel(application) {

    fun insert(uid: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            UserDatabase.getInstance(getApplication()).userDao().insert(
                User(uid.toInt(), firstName, lastName)
            )
        }
    }

    fun getAll(): Flow<List<User>> {
        return UserDatabase.getInstance(getApplication()).userDao()
            .getAll()
            .catch { e -> e.printStackTrace() }
            .flowOn(Dispatchers.IO)
    }
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", typeAffinity = ColumnInfo.INTEGER)
    val id: Int,

    @ColumnInfo(name = "first_name", typeAffinity = ColumnInfo.TEXT)
    val firstName: String,

    @ColumnInfo(name = "last_name", typeAffinity = ColumnInfo.TEXT)
    val lastName: String,
)

@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        private var instance: UserDatabase? = null

        fun getInstance(context: Context): UserDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(context, UserDatabase::class.java, "users.db").build()
                    .also { instance = it }
            }
        }
    }
}

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Query("SELECT * FROM users")
    fun getAll(): Flow<List<User>>
}

class RoomListAdapter : RecyclerView.Adapter<RoomListAdapter.ViewHolder>() {

    private val data = mutableListOf<User>()

    fun updateUsers(users: List<User>) {
        this.data.clear()
        this.data.addAll(users)
        notifyDataSetChanged()
    }

    class ViewHolder(val viewBinding: RoomItemBinding) : RecyclerView.ViewHolder(viewBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val viewBinding =
            RoomItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = data[position]
        holder.viewBinding.tvUserId.text = user.id.toString()
        holder.viewBinding.tvUserFirstName.text = user.firstName
        holder.viewBinding.tvUserLastName.text = user.lastName
    }

    override fun getItemCount(): Int {
        return data.size
    }
}
