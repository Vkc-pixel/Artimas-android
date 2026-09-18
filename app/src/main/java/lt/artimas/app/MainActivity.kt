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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg=Color.Black
private val White=Color.White
private val Green=Color(0xFF20D34A)
private val Blue=Color(0xFF2878E8)
private val Purple=Color(0xFF7A35D8)
private val Red=Color(0xFFFF3038)
private val Panel=Color(0xFF070909)
enum class Tab { WRITE, LISTEN, CHAT }

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContent{MaterialTheme{AccessibilityApp()}}
    }
}

fun vibrate(c:Context){
    c.getSystemService(Vibrator::class.java)?.vibrate(
        VibrationEffect.createOneShot(45,VibrationEffect.DEFAULT_AMPLITUDE)
    )
}

@Composable
fun OutlineButton(
    text:String,
    modifier:Modifier=Modifier,
    fontSize:Int=34,
    activeColor:Color=Bg,
    onClick:()->Unit
){
    val c=LocalContext.current
    Button(
        onClick={vibrate(c);onClick()},
        modifier=modifier,
        colors=ButtonDefaults.buttonColors(containerColor=activeColor),
        border=BorderStroke(2.dp,White),
        shape=RoundedCornerShape(7.dp),
        contentPadding=PaddingValues(horizontal=2.dp,vertical=0.dp)
    ){
        Text(text,color=White,fontSize=fontSize.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center,maxLines=1)
    }
}

@Composable
fun AccessibilityApp(){
    var tab by remember{mutableStateOf(Tab.WRITE)}
    var size by remember{mutableFloatStateOf(64f)}
    val c=LocalContext.current
    Surface(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),color=Bg){
        Column{
            Box(Modifier.weight(1f)){
                when(tab){
                    Tab.WRITE->WriteScreen(size,{size=(size+6).coerceAtMost(104f)},{size=(size-6).coerceAtLeast(36f)})
                    Tab.LISTEN->ListenScreen(size,{size=(size+6).coerceAtMost(104f)},{size=(size-6).coerceAtLeast(36f)})
                    Tab.CHAT->ChatScreen(size,{size=(size+4).coerceAtMost(80f)},{size=(size-4).coerceAtLeast(28f)})
                }
            }
            BottomNav(tab){vibrate(c);tab=it}
        }
    }
}

@Composable
fun WriteScreen(size:Float,plus:()->Unit,minus:()->Unit){
    var text by remember{mutableStateOf("")}
    var nums by remember{mutableStateOf(false)}
    var keyboardVisible by remember{mutableStateOf(true)}
    val letters=listOf(
        listOf("E","R","T","Y","U","I","O","P"),
        listOf("A","S","D","F","G","H","J","K","L"),
        listOf("Z","X","C","V","B","N","M")
    )
    val numbers=listOf(
        listOf("1","2","3","4","5"),
        listOf("6","7","8","9","0"),
        listOf(". ",",","?","!","-")
    )

    Column(Modifier.fillMaxSize().padding(horizontal=10.dp,vertical=6.dp)){
        Box(
            Modifier.weight(if(keyboardVisible) .43f else 1f)
                .fillMaxWidth()
                .border(1.dp,Color(0xFF6D6D6D),RoundedCornerShape(8.dp))
                .background(Panel,RoundedCornerShape(8.dp))
                .padding(14.dp)
        ){
            Text(
                if(text.isEmpty())"Rašykite..." else text,
                color=if(text.isEmpty())Color(0xFFB0B0B0) else White,
                fontSize=size.sp,
                fontWeight=FontWeight.SemiBold,
                lineHeight=(size*1.04f).sp
            )
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.height(58.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){
            OutlineButton("A+",Modifier.weight(1f).fillMaxHeight(),32,onClick=plus)
            OutlineButton("A−",Modifier.weight(1f).fillMaxHeight(),32,onClick=minus)
            if(keyboardVisible){
                OutlineButton("TRINTI",Modifier.weight(2f).fillMaxHeight(),28,onClick={if(text.isNotEmpty())text=text.dropLast(1)})
            } else {
                OutlineButton("RODYTI",Modifier.weight(2f).fillMaxHeight(),26,onClick={keyboardVisible=true})
            }
        }

        if(keyboardVisible){
            Spacer(Modifier.height(7.dp))
            Column(Modifier.weight(.57f),verticalArrangement=Arrangement.spacedBy(5.dp)){
                val rows=if(nums) numbers else letters
                rows.forEach{row->
                    Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                        row.forEach{x->
                            OutlineButton(
                                x.trim(),
                                Modifier.weight(1f).fillMaxHeight(),
                                fontSize=43,
                                onClick={text+=x}
                            )
                        }
                    }
                }
                Row(Modifier.height(62.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    OutlineButton(if(nums)"ABC" else "123",Modifier.weight(1.05f).fillMaxHeight(),31,onClick={nums=!nums})
                    OutlineButton("TARPAS",Modifier.weight(2.75f).fillMaxHeight(),34,onClick={text+=" "})
                    OutlineButton("SLĖPTI",Modifier.weight(1.45f).fillMaxHeight(),24,onClick={keyboardVisible=false})
                }
            }
        }
    }
}

@Composable
fun ListenScreen(size:Float,plus:()->Unit,minus:()->Unit){
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
            override fun onError(code:Int){if(listening)text="Nepavyko atpažinti kalbos. Paspauskite PRADĖTI dar kartą.";listening=false}
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
        if(granted&&startAfterPermission){startAfterPermission=false;startRecognition()}
        else if(!granted){startAfterPermission=false;text="Reikia leisti naudoti mikrofoną."}
    }
    DisposableEffect(Unit){onDispose{listening=false;r?.destroy()}}
    fun start(){
        if(c.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            startAfterPermission=true;permission.launch(Manifest.permission.RECORD_AUDIO)
        }else startRecognition()
    }
    Column(Modifier.fillMaxSize().padding(10.dp)){
        Box(Modifier.weight(1f).fillMaxWidth().border(1.dp,Color.Gray,RoundedCornerShape(8.dp)).background(Panel,RoundedCornerShape(8.dp)).padding(16.dp)){
            Text(text,color=White,fontSize=size.sp,fontWeight=FontWeight.SemiBold,lineHeight=(size*1.04f).sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.height(64.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlineButton("A−",Modifier.weight(.8f).fillMaxHeight(),30,onClick=minus)
            OutlineButton("A+",Modifier.weight(.8f).fillMaxHeight(),30,onClick=plus)
            OutlineButton(if(listening)"KLAUSAU…" else "PRADĖTI",Modifier.weight(1.6f).fillMaxHeight(),22,if(listening)Green else Green,onClick={start()})
            OutlineButton("BAIGTI",Modifier.weight(1.2f).fillMaxHeight(),24,Red,onClick={listening=false;r?.cancel()})
        }
    }
}

@Composable
fun ChatScreen(size:Float,plus:()->Unit,minus:()->Unit){
    Column(Modifier.fillMaxSize().padding(10.dp)){
        Text("Šeimos narys",color=White,fontSize=30.sp,fontWeight=FontWeight.SemiBold)
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.Bottom){
            Text("Kaip jautiesi?\nAr jau pavalgei?",color=White,fontSize=size.sp,lineHeight=(size*1.04f).sp,modifier=Modifier.border(1.dp,White,RoundedCornerShape(8.dp)).padding(12.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.height(64.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlineButton("A−",Modifier.weight(.8f).fillMaxHeight(),30,onClick=minus)
            OutlineButton("A+",Modifier.weight(.8f).fillMaxHeight(),30,onClick=plus)
            OutlineButton("ATSAKYTI BALSU",Modifier.weight(2.6f).fillMaxHeight(),22,Red,onClick={})
        }
    }
}

@Composable
fun BottomNav(tab:Tab,onTab:(Tab)->Unit){
    Row(
        Modifier.fillMaxWidth().padding(start=8.dp,end=8.dp,top=8.dp,bottom=5.dp).height(72.dp),
        horizontalArrangement=Arrangement.spacedBy(6.dp)
    ){
        listOf(Tab.WRITE to "RAŠYTI",Tab.LISTEN to "KLAUSYTI",Tab.CHAT to "POKALBIS").forEach{(t,label)->
            val selected=tab==t
            Button(
                onClick={onTab(t)},
                modifier=Modifier.weight(1f).fillMaxHeight(),
                colors=ButtonDefaults.buttonColors(containerColor=if(selected)when(t){Tab.WRITE->Green;Tab.LISTEN->Blue;Tab.CHAT->Purple}else Bg),
                border=BorderStroke(2.dp,White),
                shape=RoundedCornerShape(7.dp),
                contentPadding=PaddingValues(1.dp)
            ){
                Text(label,color=White,fontSize=23.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
            }
        }
    }
}
