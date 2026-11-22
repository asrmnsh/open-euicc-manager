
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
  String packageName = '';
  String sha1 = '';
  String sha256 = '';

  bool loading = false;

  Future<void> checkDevice() async {
    setState(() {
      loading = true;
      result = 'جاري الفحص...';
    });

    try {
      final response =
          await platform.invokeMapMethod<String, dynamic>('discover');

      final supported =
          response?['omapiSupported'] == true;

      final readers =
          (response?['readers'] as List?)
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
          await platform.invokeMapMethod<String, dynamic>('testSim1');

      setState(() {
        result = response?['message']?.toString()
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

  // استخراج شهادة توقيع التطبيق
  Future<void> getAppCertificate() async {
    setState(() {
      loading = true;
      result = 'جاري استخراج شهادة التطبيق...';
    });

    try {
      final response =
          await platform.invokeMapMethod<String, dynamic>(
        'getAppCertificate',
      );

      final certificates =
          response?['certificates'] as List?;

      if (certificates == null || certificates.isEmpty) {
        throw Exception('لم يتم العثور على شهادة توقيع');
      }

      final certificate =
          Map<String, dynamic>.from(certificates.first as Map);

      setState(() {
        packageName =
            response?['packageName']?.toString() ?? '';

        sha1 = certificate['SHA1']?.toString() ?? '';
        sha256 = certificate['SHA256']?.toString() ?? '';

        result = 'تم استخراج شهادة توقيع التطبيق بنجاح';
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

  Future<void> copyValue(String value) async {
    await Clipboard.setData(
      ClipboardData(text: value.replaceAll(':', '')),
    );

    if (!mounted) return;

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('تم نسخ البصمة'),
        duration: Duration(seconds: 2),
      ),
    );
  }

  Widget certificateField(
    String title,
    String value,
  ) {
    if (value.isEmpty) {
      return const SizedBox.shrink();
    }

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 16,
              ),
            ),
            const SizedBox(height: 8),
            SelectableText(
              value,
              style: const TextStyle(fontSize: 12),
            ),
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton.icon(
                onPressed: () => copyValue(value),
                icon: const Icon(Icons.copy),
                label: const Text('نسخ'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Open eUICC Manager'),
      ),
      body: SingleChildScrollView(
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
              'فحص وإدارة شرائح eUICC',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
              ),
            ),

            const SizedBox(height: 24),

            ElevatedButton(
              onPressed: loading ? null : checkDevice,
              child: const Text('افحص الجهاز'),
            ),

            const SizedBox(height: 12),

            ElevatedButton(
              onPressed: loading ? null : testSim1,
              child: const Text('اختبار شريحة SIM1'),
            ),

            const SizedBox(height: 12),

            ElevatedButton.icon(
              onPressed: loading ? null : getAppCertificate,
              icon: const Icon(Icons.verified_user),
              label: const Text('استخراج شهادة توقيع التطبيق'),
            ),

            const SizedBox(height: 24),

            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: SelectableText(
                  result,
                  textAlign: TextAlign.center,
                  style: const TextStyle(fontSize: 16),
                ),
              ),
            ),

            if (packageName.isNotEmpty) ...[
              const SizedBox(height: 16),

              const Text(
                'بيانات شهادة التطبيق',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),

              const SizedBox(height: 8),

              SelectableText('Package: $packageName'),

              certificateField('SHA-1', sha1),
              certificateField('SHA-256', sha256),
            ],
          ],
        ),
      ),
    );
  }
}
