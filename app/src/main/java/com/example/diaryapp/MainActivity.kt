package com.example.diaryapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diaryapp.ui.theme.DiaryAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiaryItem(
    val id: Int,
    val title: String,
    val content: String,
    val dateTime: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiaryAppTheme {
                DiaryAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryAppScreen() {
    val context = LocalContext.current
    val dbHelper = remember { SQLiteHelper(context) }

    var diaryList by remember { mutableStateOf(listOf<DiaryItem>()) }
    val selectedIds = remember { mutableStateListOf<Int>() }

    var isSearchVisible by remember { mutableStateOf(false) } // Trạng thái ẩn/hiện tìm kiếm
    var searchQuery by remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editingDiary by remember { mutableStateOf<DiaryItem?>(null) }

    fun loadDiaries() {
        diaryList = if (searchQuery.isBlank()) {
            val list = mutableListOf<DiaryItem>()
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT * FROM diaries ORDER BY id DESC", null)
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                    val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                    val content = cursor.getString(cursor.getColumnIndexOrThrow("content"))
                    val dateTime = cursor.getString(cursor.getColumnIndexOrThrow("date_time"))
                    list.add(DiaryItem(id, title, content, dateTime))
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
            list
        } else {
            dbHelper.searchDiaries(searchQuery)
        }
    }

    LaunchedEffect(searchQuery) {
        loadDiaries()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectedIds.isNotEmpty()) {
                        Text(
                            text = "${selectedIds.size} mục được chọn",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Nhật ký đa phương tiện",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = { selectedIds.clear() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Hủy chọn",
                                tint = Color.White
                            )
                        }
                    }
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color.White)
                        }
                    } else {
                        // Nút bật/tắt ô tìm kiếm
                        IconButton(onClick = {
                            isSearchVisible = !isSearchVisible
                            if (!isSearchVisible) searchQuery = "" // Reset tìm kiếm khi ẩn
                        }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Tìm kiếm",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF00897B))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingDiary = null
                    showDialog = true
                },
                containerColor = Color(0xFFE91E63),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm mới")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Thanh Tìm kiếm với hiệu ứng xuất hiện / biến mất mượt mà
            AnimatedVisibility(
                visible = isSearchVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    placeholder = { Text("Tìm kiếm nhật ký...", fontSize = 16.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Tìm kiếm", tint = Color.Gray)
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchQuery.isNotEmpty()) {
                                searchQuery = ""
                            } else {
                                isSearchVisible = false
                            }
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Đóng", tint = Color.Gray)
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp)
                )
            }

            // Danh sách nhật ký
            Box(modifier = Modifier.weight(1f)) {
                if (diaryList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isEmpty()) "Chưa có nhật ký nào" else "Không tìm thấy kết quả phù hợp",
                            color = Color.Gray,
                            fontSize = 18.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(diaryList, key = { it.id }) { diary ->
                            DiaryCard(
                                diary = diary,
                                isSelected = selectedIds.contains(diary.id),
                                onCardClick = {
                                    if (selectedIds.isNotEmpty()) {
                                        if (selectedIds.contains(diary.id)) selectedIds.remove(diary.id)
                                        else selectedIds.add(diary.id)
                                    } else {
                                        editingDiary = diary
                                        showDialog = true
                                    }
                                },
                                onCardLongClick = {
                                    if (!selectedIds.contains(diary.id)) selectedIds.add(diary.id)
                                },
                                onShareClick = {
                                    shareDiary(context, diary)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showDialog) {
            DiaryDialog(
                diary = editingDiary,
                onDismiss = { showDialog = false },
                onSave = { title, content ->
                    val currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                    if (editingDiary == null) {
                        dbHelper.insertDiary(title, content, currentTime)
                    } else {
                        dbHelper.updateDiary(editingDiary!!.id, title, content, currentTime)
                    }
                    loadDiaries()
                    showDialog = false
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text(
                        text = "Xác nhận xóa",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Bạn có chắc chắn muốn xóa ${selectedIds.size} mục đã chọn?",
                        fontSize = 16.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            dbHelper.deleteDiaries(selectedIds.toList())
                            selectedIds.clear()
                            loadDiaries()
                            showDeleteDialog = false
                            Toast.makeText(context, "Đã xóa thành công!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Xóa", fontSize = 16.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Hủy", fontSize = 16.sp)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiaryCard(
    diary: DiaryItem,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onCardLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE0F2F1) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = diary.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF212121)
                )

                if (diary.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = diary.content,
                        fontSize = 16.sp,
                        color = Color(0xFF616161),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = diary.dateTime,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Chia sẻ",
                    tint = Color(0xFF00897B),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun DiaryDialog(
    diary: DiaryItem?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(diary?.title ?: "") }
    var content by remember { mutableStateOf(diary?.content ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (diary == null) "Thêm Nhật ký" else "Cập nhật Nhật ký",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề", fontSize = 16.sp) },
                    textStyle = TextStyle(fontSize = 18.sp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Chi tiết", fontSize = 16.sp) },
                    textStyle = TextStyle(fontSize = 18.sp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onSave(title, content)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
            ) {
                Text("Lưu", fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", fontSize = 16.sp)
            }
        }
    )
}

fun shareDiary(context: Context, diary: DiaryItem) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(
            Intent.EXTRA_TEXT,
            "【${diary.title}】\nThời gian: ${diary.dateTime}\n\nNội dung:\n${diary.content}"
        )
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Chia sẻ nhật ký qua")
    context.startActivity(shareIntent)
}