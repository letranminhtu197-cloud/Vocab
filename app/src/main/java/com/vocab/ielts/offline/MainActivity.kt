package com.vocab.ielts.offline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Word(val term:String,val ipa:String,val pos:String,val meaning:String,val example:String,val topic:String)
private val seed = listOf(
 Word("address an issue","/əˈdres ən ˈɪʃuː/","phrase","giải quyết một vấn đề","Governments should address this issue urgently.","Society"),
 Word("allocate","/ˈæləkeɪt/","verb","phân bổ","The city should allocate more funds to public transport.","Economy"),
 Word("beneficial","/ˌbenɪˈfɪʃəl/","adjective","có lợi","Regular exercise is beneficial to mental health.","Health"),
 Word("crucial","/ˈkruːʃəl/","adjective","then chốt, vô cùng quan trọng","Education plays a crucial role in social mobility.","Education"),
 Word("deteriorate","/dɪˈtɪəriəreɪt/","verb","xấu đi, suy giảm","Air quality may deteriorate without effective policies.","Environment"),
 Word("evidence","/ˈevɪdəns/","noun","bằng chứng","There is strong evidence that early education matters.","Education"),
 Word("feasible","/ˈfiːzəbəl/","adjective","khả thi","A fully car-free city may not be feasible immediately.","Cities"),
 Word("impact","/ˈɪmpækt/","noun/verb","tác động, ảnh hưởng","Technology has a significant impact on employment.","Technology"),
 Word("mitigate","/ˈmɪtɪɡeɪt/","verb","giảm nhẹ, làm dịu","Governments must mitigate the effects of climate change.","Environment"),
 Word("sustainable","/səˈsteɪnəbəl/","adjective","bền vững","Cities need sustainable transport systems.","Environment"),
 Word("pose a threat","/pəʊz ə θret/","phrase","gây ra mối đe dọa","Plastic waste poses a threat to marine life.","Environment"),
 Word("raise awareness","/reɪz əˈweənəs/","phrase","nâng cao nhận thức","Schools can raise awareness of healthy habits.","Health")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { VocabApp() } }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun VocabApp() {
    var tab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var current by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var learned by remember { mutableIntStateOf(0) }
    val words = remember { mutableStateListOf<Word>().apply { addAll(seed) } }
    val filtered = words.filter { it.term.contains(query,true) || it.meaning.contains(query,true) || it.topic.contains(query,true) }
    MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF356859), secondary = androidx.compose.ui.graphics.Color(0xFFF2B544), background = androidx.compose.ui.graphics.Color(0xFFF6F8F5))) {
        Scaffold(
            topBar = { TopAppBar(title={ Column { Text("Vocab",fontWeight=FontWeight.Bold); Text("IELTS Vocabulary • Offline",fontSize=12.sp) } }) },
            bottomBar = { NavigationBar {
                listOf("Home","Lộ trình","Ôn tập","Sổ tay","Thống kê").forEachIndexed { i,label ->
                    NavigationBarItem(selected=tab==i,onClick={tab=i},icon={ Icon(when(i){0->Icons.Default.Home;1->Icons.Default.Route;2->Icons.Default.Refresh;3->Icons.Default.MenuBook;else->Icons.Default.BarChart},"") },label={Text(label)})
                }
            } }
        ) { padding ->
            when(tab) {
                0 -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    item { Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primary),shape=RoundedCornerShape(24.dp)) { Column(Modifier.padding(20.dp)) { Text("Hôm nay, học một chút nhé!",color=MaterialTheme.colorScheme.onPrimary,fontSize=20.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("Từ vựng khởi tạo: ${words.size}  •  Lượt học: $learned",color=MaterialTheme.colorScheme.onPrimary); LinearProgressIndicator(progress={ (learned.coerceAtMost(10)/10f) },modifier=Modifier.fillMaxWidth().padding(top=12.dp)) } } }
                    item { Text("Tiếp tục học",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold) }
                    item { Button(onClick={tab=2},modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.Refresh,null); Spacer(Modifier.width(8.dp)); Text("Ôn tập từ vựng") } }
                    item { OutlinedButton(onClick={tab=1},modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.School,null); Spacer(Modifier.width(8.dp)); Text("Bắt đầu lộ trình") } }
                    item { Text("Nguyên tắc học",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold) }
                    item { Text("Gợi nhớ chủ động • Ôn cách quãng • Học từ trong ngữ cảnh",color=MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                1 -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    item { Text("Lộ trình IELTS",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold) }
                    listOf("Education","Environment","Technology","Health","Economy","Society","Cities").forEachIndexed { i,t -> item { Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)) { Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(t,fontWeight=FontWeight.SemiBold); Text("Chặng ${i+1} • học theo ngữ cảnh",style=MaterialTheme.typography.bodySmall) }; Text("→") } } } }
                }
                2 -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("Ôn tập flashcard",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                    Text("Thẻ ${(current % words.size)+1} / ${words.size}")
                    Card(Modifier.fillMaxWidth().weight(1f),shape=RoundedCornerShape(24.dp)) { Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(words[current % words.size].term,fontSize=28.sp,fontWeight=FontWeight.Bold)
                        Text(words[current % words.size].pos,color=MaterialTheme.colorScheme.primary)
                        if(revealed) { Text(words[current % words.size].meaning,fontSize=22.sp); Spacer(Modifier.height(12.dp)); Text(words[current % words.size].example) }
                        Spacer(Modifier.height(20.dp)); OutlinedButton(onClick={revealed=!revealed}) { Text(if(revealed) "Ẩn đáp án" else "Hiện đáp án") }
                    } }
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick={current=(current-1+words.size)%words.size;revealed=false},modifier=Modifier.weight(1f)){Text("Trước")}
                        Button(onClick={current=(current+1)%words.size;revealed=false;learned++},modifier=Modifier.weight(1f)){Text("Đã ôn")}
                    }
                }
                3 -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    Text("Sổ tay từ vựng",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),label={Text("Tìm từ, nghĩa hoặc chủ đề")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
                    LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(top=10.dp)) { items(filtered) { w -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text(w.term,fontWeight=FontWeight.Bold,fontSize=18.sp); Text("${w.ipa} • ${w.pos}",style=MaterialTheme.typography.bodySmall); Text(w.meaning); Text(w.example,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant); Text(w.topic,style=MaterialTheme.typography.labelSmall) } } } }
                }
                else -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    item { Text("Thống kê",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold) }
                    item { StatCard("Tổng số mục từ",words.size.toString()) }
                    item { StatCard("Lượt đã đánh dấu ôn",learned.toString()) }
                    item { StatCard("Chế độ hoạt động","Ngoại tuyến") }
                    item { Text("Đây là bản nền ban đầu. Lịch sử và lịch SRS bền vững sẽ được bổ sung trong các bước tiếp theo.",style=MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
}
@Composable private fun StatCard(label:String,value:String) { Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)) { Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically) { Text(label,Modifier.weight(1f)); Text(value,fontWeight=FontWeight.Bold,fontSize=22.sp,color=MaterialTheme.colorScheme.primary) } } }
