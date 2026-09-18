package lt.artimas.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg=Color.Black; private val White=Color.White; private val Green=Color(0xFF20D34A); private val Blue=Color(0xFF2878E8); private val Purple=Color(0xFF7A35D8); private val Red=Color(0xFFFF3038); private val Key=Color(0xFF171C20)
enum class Tab { WRITE, LISTEN, CHAT }
class MainActivity:ComponentActivity(){ override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MaterialTheme{AccessibilityApp()}}}}
fun vibrate(c:Context){c.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(45,VibrationEffect.DEFAULT_AMPLITUDE))}
@Composable fun BigButton(text:String,modifier:Modifier=Modifier,color:Color=Key,onClick:()->Unit){val c=LocalContext.current;Button(onClick={vibrate(c);onClick()},modifier=modifier.heightIn(min=62.dp),colors=ButtonDefaults.buttonColors(containerColor=color),shape=RoundedCornerShape(10.dp),contentPadding=PaddingValues(4.dp)){Text(text,color=White,fontSize=24.sp,fontWeight=FontWeight.Bold)}}
@Composable fun AccessibilityApp(){var tab by remember{mutableStateOf(Tab.WRITE)};var size by remember{mutableFloatStateOf(48f)};val c=LocalContext.current;Surface(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),color=Bg){Column{Box(Modifier.weight(1f)){when(tab){Tab.WRITE->WriteScreen(size,{size=(size+4).coerceAtMost(88f)},{size=(size-4).coerceAtLeast(28f)});Tab.LISTEN->ListenScreen(size,{size=(size+4).coerceAtMost(88f)},{size=(size-4).coerceAtLeast(28f)});Tab.CHAT->ChatScreen(size,{size=(size+4).coerceAtMost(72f)},{size=(size-4).coerceAtLeast(24f)})};Text("⚙",color=White,fontSize=28.sp,modifier=Modifier.align(Alignment.TopEnd).padding(14.dp))};BottomNav(tab){vibrate(c);tab=it}}}}
@Composable fun WriteScreen(size:Float,plus:()->Unit,minus:()->Unit){var text by remember{mutableStateOf("")};var nums by remember{mutableStateOf(false)};val rows=if(nums)listOf(listOf("1","2","3","4","5"),listOf("6","7","8","9","0"))else listOf(listOf("Ą","Č","Ę","Ė","Į","Š","Ų","Ū","Ž"),listOf("A","B","C","D","E","F","G","H","I"),listOf("J","K","L","M","N","O","P","R","S"),listOf("T","U","V","Z"));Column(Modifier.fillMaxSize().padding(12.dp)){Box(Modifier.weight(.40f).fillMaxWidth().background(Color(0xFF070909),RoundedCornerShape(12.dp)).padding(14.dp)){Text(if(text.isEmpty())"Rašykite..." else text,color=if(text.isEmpty())Color.Gray else White,fontSize=size.sp,fontWeight=FontWeight.Bold,lineHeight=(size*1.08f).sp)};Spacer(Modifier.height(8.dp));Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){BigButton("A+",Modifier.weight(1f),onClick=plus);BigButton("A−",Modifier.weight(1f),onClick=minus);BigButton("⌫",Modifier.weight(1f),onClick={if(text.isNotEmpty())text=text.dropLast(1)});BigButton("123",Modifier.weight(1f),onClick={nums=!nums})};Spacer(Modifier.height(7.dp));Column(Modifier.weight(.60f),verticalArrangement=Arrangement.spacedBy(5.dp)){rows.forEach{row->Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(4.dp)){row.forEach{x->BigButton(x,Modifier.weight(1f).fillMaxHeight(),onClick={text+=x.lowercase()})}}};Row(Modifier.weight(1.05f),horizontalArrangement=Arrangement.spacedBy(6.dp)){BigButton("TARPAS",Modifier.weight(3f).fillMaxHeight(),onClick={text+=" "});BigButton("ENTER",Modifier.weight(1.5f).fillMaxHeight(),onClick={text+="\n"})}}}}
@Composable fun ListenScreen(size:Float,plus:()->Unit,minus:()->Unit){
    val c=LocalContext.current
    var text by remember{mutableStateOf("Paspauskite PRADĖTI ir kalbėkite.")}
    var r by remember{mutableStateOf<SpeechRecognizer?>(null)}
    var listening by remember{mutableStateOf(false)}
    var startAfterPermission by remember{mutableStateOf(false)}
    fun startRecognition(){
        if(!SpeechRecognizer.isRecognitionAvailable(c)){text="Kalbos atpažinimas šiame telefone nepasiekiamas.";return}
        r?.destroy()
        r=SpeechRecognizer.createSpeechRecognizer(c)
        r?.setRecognitionListener(object:RecognitionListener{
            override fun onReadyForSpeech(p0:Bundle?){text="Klausau…"}
            override fun onBeginningOfSpeech(){}
            override fun onRmsChanged(p0:Float){}
            override fun onBufferReceived(p0:ByteArray?){}
            override fun onEndOfSpeech(){}
            override fun onError(code:Int){if(listening) text="Nepavyko atpažinti kalbos. Paspauskite PRADĖTI dar kartą.";listening=false}
            override fun onResults(b:Bundle?){b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let{text=it};listening=false}
            override fun onPartialResults(b:Bundle?){b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let{text=it}}
            override fun onEvent(p0:Int,p1:Bundle?){}
        })
        val i=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,"lt-LT")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"lt-LT")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
        }
        listening=true
        r?.startListening(i)
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        if(granted && startAfterPermission){startAfterPermission=false;startRecognition()}
        else if(!granted){startAfterPermission=false;text="Reikia leisti naudoti mikrofoną."}
    }
    DisposableEffect(Unit){onDispose{listening=false;r?.destroy()}}
    fun start(){
        if(c.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            startAfterPermission=true;permission.launch(Manifest.permission.RECORD_AUDIO)
        } else startRecognition()
    }
    Column(Modifier.fillMaxSize().padding(12.dp)){
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF070909),RoundedCornerShape(12.dp)).padding(16.dp)){
            Text(text,color=White,fontSize=size.sp,fontWeight=FontWeight.Bold,lineHeight=(size*1.08f).sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
            BigButton("A−",Modifier.weight(.8f),onClick=minus)
            BigButton("A+",Modifier.weight(.8f),onClick=plus)
            BigButton(if(listening)"KLAUSAU…" else "PRADĖTI",Modifier.weight(1.6f),Green,onClick={start()})
            BigButton("BAIGTI",Modifier.weight(1.6f),Red,onClick={listening=false;r?.cancel();text=if(text=="Klausau…")"Klausymas sustabdytas." else text})
        }
    }
}
@Composable fun ChatScreen(size:Float,plus:()->Unit,minus:()->Unit){Column(Modifier.fillMaxSize().padding(12.dp)){Text("Šeimos narys",color=White,fontSize=28.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(6.dp));Column(Modifier.weight(1f),verticalArrangement=Arrangement.Bottom){Text("Kaip jautiesi?\nAr jau pavalgei?",color=White,fontSize=size.sp,lineHeight=(size*1.08f).sp,modifier=Modifier.background(Key,RoundedCornerShape(14.dp)).padding(14.dp));Spacer(Modifier.height(12.dp));Text("Taip, viskas gerai.\nJau pavalgiau.",color=White,fontSize=size.sp,lineHeight=(size*1.08f).sp,modifier=Modifier.background(Color(0xFF174A28),RoundedCornerShape(14.dp)).padding(14.dp))};Spacer(Modifier.height(8.dp));Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){BigButton("A−",Modifier.weight(.8f),onClick=minus);BigButton("A+",Modifier.weight(.8f),onClick=plus);BigButton("🎤 ATSAKYTI BALSU",Modifier.weight(2.7f),Red,onClick={})}}}
@Composable fun BottomNav(tab:Tab,onTab:(Tab)->Unit){Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(Tab.WRITE to "⌨\nRašyti",Tab.LISTEN to "🎤\nKlausyti",Tab.CHAT to "●●\nPokalbis").forEach{(t,label)->val col=when(t){Tab.WRITE->Green;Tab.LISTEN->Blue;Tab.CHAT->Purple};Button(onClick={onTab(t)},Modifier.weight(1f).height(72.dp),colors=ButtonDefaults.buttonColors(containerColor=if(tab==t)col else Key),shape=RoundedCornerShape(10.dp),contentPadding=PaddingValues(2.dp)){Text(label,color=White,fontSize=17.sp,fontWeight=FontWeight.Bold)}}}}
