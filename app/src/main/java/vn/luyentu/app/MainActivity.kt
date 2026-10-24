package vn.luyentu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LuyenTuApp() }
    }
}

private data class Destination(val title: String, val icon: ImageVector)

private val destinations = listOf(
    Destination("Tổng quan", Icons.Default.Home),
    Destination("Sổ từ", Icons.Default.AutoStories),
    Destination("Luyện tập", Icons.Default.School),
    Destination("Thống kê", Icons.Default.BarChart),
    Destination("Cài đặt", Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LuyenTuApp() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("LuyenTu 2.0", fontWeight = FontWeight.Bold)
                                Text("Không gian học từ vựng cá nhân", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar {
                        destinations.forEachIndexed { index, destination ->
                            NavigationBarItem(
                                selected = selected == index,
                                onClick = { selected = index },
                                icon = { androidx.compose.material3.Icon(destination.icon, contentDescription = destination.title) },
                                label = { Text(destination.title) }
                            )
                        }
                    }
                }
            ) { padding ->
                when (selected) {
                    0 -> HomeScreen(padding)
                    1 -> PlaceholderScreen(padding, "Sổ từ", "Nơi quản lý từ vựng do bạn tự nhập.")
                    2 -> PlaceholderScreen(padding, "Luyện tập", "Các bài tập chủ động và hệ thống SRS sẽ được triển khai ở các bước tiếp theo.")
                    3 -> PlaceholderScreen(padding, "Thống kê", "Theo dõi tiến độ học tập được lưu cục bộ.")
                    else -> PlaceholderScreen(padding, "Cài đặt", "Thiết lập trải nghiệm học offline và sao lưu dữ liệu.")
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("Bắt đầu từ vựng của riêng bạn", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("LuyenTu 2.0 được xây dựng theo hướng offline-first. Bạn sẽ tự nhập bộ từ của mình; dữ liệu học không cần tài khoản hay máy chủ.")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Tổng số từ", "0", Modifier.weight(1f))
                MetricCard("Đến hạn ôn", "0", Modifier.weight(1f))
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Lộ trình triển khai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("1. Nền tảng Android và giao diện")
                    Text("2. Cơ sở dữ liệu cục bộ và nhập CSV")
                    Text("3. Các dạng bài tập và SRS")
                    Text("4. Sao lưu, kiểm thử offline và build APK")
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PlaceholderScreen(padding: PaddingValues, title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.bodyLarge)
        Text("Màn hình này hiện là khung giao diện, chưa phải tính năng hoàn chỉnh.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
