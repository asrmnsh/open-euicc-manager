
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const EuiccApp());
}

class EuiccApp extends StatelessWidget {
  const EuiccApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Open eUICC Manager',
      theme: ThemeData(
        colorSchemeSeed: Colors.teal,
        useMaterial3: true,
      ),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  static const platform =
      MethodChannel('org.openeuicc.manager/bridge');

  String result = 'لم يتم فحص الجهاز بعد';
  bool loading = false;

  Future<void> checkDevice() async {
    setState(() {
      loading = true;
      result = 'جاري الفحص...';
    });

    try {
      final response =
          await platform.invokeMethod('discover');

      final supported =
          response['omapiSupported'] == true;

      final readers =
          (response['readers'] as List?)
                  ?.map((e) => e.toString())
                  .toList() ??
              [];

      setState(() {
        result = 'دعم OMAPI: ${supported ? "نعم" : "غير مؤكد"}\n'
            'عدد القارئات: ${readers.length}\n'
            'القارئات: ${readers.isEmpty ? "لم يتم العثور عليها" : readers.join(", ")}';
      });
    } on PlatformException catch (e) {
      setState(() {
        result = 'خطأ: ${e.message}';
      });
    } catch (e) {
      setState(() {
        result = 'حدث خطأ: $e';
      });
    } finally {
      setState(() {
        loading = false;
      });
    }
  }

  Future<void> testSim1() async {
    setState(() {
      loading = true;
      result = 'جاري اختبار SIM1...';
    });

    try {
      final response =
          await platform.invokeMethod('testSim1');

      setState(() {
        result = response['message']?.toString()
            ?? 'لم تصل نتيجة الاختبار';
      });
    } on PlatformException catch (e) {
      setState(() {
        result = 'خطأ: ${e.message}';
      });
    } catch (e) {
      setState(() {
        result = 'حدث خطأ: $e';
      });
    } finally {
      setState(() {
        loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Open eUICC Manager'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Icon(
              Icons.sim_card,
              size: 80,
              color: Colors.teal,
            ),
            const SizedBox(height: 20),
            const Text(
              'فحص دعم شرائح eUICC',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: loading ? null : checkDevice,
              child: Text(
                loading ? 'جاري الفحص...' : 'افحص الجهاز',
              ),
            ),
            const SizedBox(height: 12),
            ElevatedButton(
              onPressed: loading ? null : testSim1,
              child: const Text('اختبار شريحة SIM1'),
            ),
            const SizedBox(height: 24),
            SelectableText(
              result,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 16),
            ),
          ],
        ),
      ),
    );
  }
}

