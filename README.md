# 🖥️ Personel Yönetim Sistemi

## 📌 Projenin Amacı

Bu masaüstü uygulaması, bir yazılım şirketinin **personel yönetimini** kolaylaştırmak ve **verimliliği artırmak** amacıyla geliştirilmiştir. **Çalışan bilgileri**, **maaş hesaplama**, **çalışma saatleri**, **giriş/çıkış takibi**, **izin yönetimi** ve **raporlama** gibi işlemler, kullanıcı dostu bir arayüz ile yönetilebilir.

---
## 🛠 **Kullanılan Teknolojiler**  

- **Java** → Ana programlama dili
- **Maven** → Proje bağımlılık yönetimi ve otomatik derleme  
- **Swing** → Masaüstü arayüz tasarımı  
- **JSON** → Veri depolama ve işleme  

---

## 🚀 Özellikler

### 🏢 Kullanıcı Rolleri ve Yetkiler

- **👨‍💼 Yönetici (Admin)**

  - Personel ekleme, güncelleme ve silme
  - Çalışanların giriş/çıkış saatlerini görüntüleme
  - İzin taleplerini onaylama veya reddetme
  - Çalışma saatleri, fazla veya eksik mesai ve izinler hakkında rapor oluşturma

- **👷 Personel (Çalışan)**

  - Kendi bilgilerini görüntüleme
  - Aylık kazanç görme 
  - Günlük giriş/çıkış işlemleri yapma
  - İzin talebinde bulunma
  - Kişisel çalışma saatleri, fazla veya eksik mesai ve izinler hakkında rapor oluşturma

---

## 🖥️ Kullanıcı Arayüzü

### 📌 Giriş Ekranı

- Kullanıcı adı ve şifre ile giriş yapılır.
- Kullanıcı rolü belirlenerek, uygun arayüz yüklenir.

### 📌 Ana Ekran

**Yönetici için:**

- **📋 Personel Yönetimi**: Personel ekleme, güncelleme ve silme işlemleri
- **⏳ Giriş/Çıkış Takibi**: Çalışanların mesai saatlerini görüntüleme
- **📅 İzin Yönetimi**: Gelen izin taleplerini onaylama veya reddetme
- **📊 Raporlar**: Çalışma saatleri ve fazla mesai analizleri

**Personel için:**

- **🆔 Bilgilerim**: Kişisel bilgiler ve iş detayları
- **💰 Kazanç Bilgileri**: Maaş ve ek ödemeler
- **🕘 Giriş/Çıkış**: Günlük mesai işlemleri ve çalışma süresi sayacı
- **📝 İzin Talepleri**: Yeni izin talebi oluşturma ve geçmiş talepleri görüntüleme
- **📄 Raporlar**: Kendi çalışma saatlerini ve fazla/eksik mesailerini analiz etme

---

## 💾 Veri Depolama

- **JSON Dosyaları** kullanılarak personel bilgileri, giriş/çıkış kayıtları ve izin talepleri saklanır.
- **Veri Dosyaları:**
  - `employees.json` → Personel bilgileri
  - `time_entries.json` → Giriş/çıkış kayıtları
  - `leave_requests.json` → İzin talepleri
- **Veri Güvenliği:**
  - Kullanıcı şifreleri **hashlenmiş** olarak saklanır.
  - **Rol bazlı erişim kontrolü** uygulanır.
  - **Thread-safe veri yapıları** kullanılarak veri bütünlüğü sağlanır.

---

## ⚙️ Kurulum ve Çalıştırma

### Gerekli Araçlar:

- Java
- Maven

### Kurulum Adımları:

1️⃣ **Projeyi Klonlayın:**

```
git clone https://github.com/semhtemiz/personnel_management_system.git
```

2️⃣ **Bağımlılıkları Yükleyin:**

```
mvn install
```

3️⃣ **Projeyi Derleyin ve Çalıştırın:**

```
mvn compile
mvn exec:java -Dexec.mainClass="com.semihtemiz.pms.Main"
```

**İlk Giriş Bilgileri:**

- **Kullanıcı Adı:** `admin`
- **Şifre:** `admin`

---

## 📊 Raporlama Sistemi

📌 **Yönetici ve Personel İçin Mevcut Raporlar:**

- Çalışma saatleri
- Departman bazlı çalışma raporu
- Fazla ya da eksik mesai analizleri
- İzin kullanım raporları


📌 **Özel Tarih Aralıklarıyla Rapor Alma**\
Yönetici ve personel, belirli tarihler arasında **çalışma saatleri** ve **izin geçmişlerini** inceleyebilir.

---

## 💰 Maaş Hesaplama

📌 **Maaş hesaplaması aşağıdaki formüle göre yapılır:**

- **Aylık Maaş** yönetici tarafından belirlenir.
- **Fazla Mesai Ücreti** = (Saatlik Ücret) x 1.5 x (Fazla Çalışma Süresi)
- **Eksik Mesai Kesintisi** = (Saatlik Ücret) x (Eksik Çalışma Süresi)

📌 Çalışanlar, giriş/çıkış saatlerini sisteme ekleyerek **maaş hesaplamalarını** görebilir.

---

## 📅 İzin Alma

📌 **İzin talebi şu adımlarla oluşturulur:**

- Çalışan, izin tarihlerini ve nedenini seçerek talepte bulunur.
- Yönetici, gelen talepleri onaylar veya reddeder.
- Onaylanan izinler **izin günlerinden düşülerek** güncellenir.
- Çalışanlar geçmiş izin taleplerini görüntüleyebilir.

📌 **İzin süreleri JSON dosyalarında saklanır ve kullanıcı profiline göre takip edilir.**

---

## 📂 Proje Yapısı

```
personnel_management_system/
│
├── data/                           # JSON veri dosyaları
│   ├── employees.json              # Personel bilgileri
│   ├── time_entries.json           # Giriş/çıkış kayıtları
│   └── leave_requests.json         # İzin talepleri
│
├── src/main/java/com/semihtemiz/pms/ # Ana kaynak kodları
│   ├── Main.java                   # Uygulamanın giriş noktası
│   ├── model/                      # Veri modelleri
│   │   ├── Employee.java
│   │   ├── TimeEntry.java
│   │   └── LeaveRequest.java
│   ├── service/                    # İş mantığı ve veri yönetimi
│   │   └── DataStorageService.java
│   └── ui/                         # Kullanıcı arayüzü
│       ├── LoginFrame.java         # Giriş ekranı
│       └── MainFrame.java          # Ana uygulama ekranı
│
└── pom.xml                         # Maven yapılandırma dosyası
```
---
### 📦 **pom.xml**  
`pom.xml`, **Maven tarafından kullanılan proje yapılandırma dosyasıdır.**  

- Projeye dahil edilecek **bağımlılıkları** (kütüphaneleri) tanımlar.  
- Derleme, test ve dağıtım süreçlerini **otomatikleştirir.**  
- **Eklentiler (plugins)** ile derleyici ayarlarını yönetir.
---

## 🔒 Güvenlik ve Hata Yönetimi

- **Şifreleme**: Kullanıcı şifreleri **hashlenerek** saklanır.
- **Rol Bazlı Yetkilendirme**: Yönetici ve personel ayrı erişim seviyelerine sahiptir.
- **Veri Doğrulama**: Kullanıcı girişlerinde ve izin taleplerinde geçerli formatlar zorunludur.
- **Otomatik Yedekleme**: JSON dosyaları periyodik olarak yedeklenir.

---

## 📌 **Yapılabilecek İyileştirmeler ve Güncellemeler**  

### 1️⃣ **Veri Depolama ve Güvenlik**  
✅ JSON yerine **SQL tabanlı bir veritabanına** geçiş yaparak daha büyük veri setlerini yönetmek mümkün olabilir. (Örn: MySQL, PostgreSQL)  
✅ **Veri şifreleme** için daha güçlü bir **hashleme algoritması** kullanılabilir.  
✅ **Otomatik yedekleme mekanizması** geliştirilebilir (örn: günlük veya haftalık yedekleme).  

### 2️⃣ **Gelişmiş Yetkilendirme ve Rol Yönetimi**  
✅ Mevcut kullanıcı rollerine ek olarak **özelleştirilebilir yetki seviyeleri** tanımlanabilir.    

### 3️⃣ **Kullanıcı Arayüzü ve Deneyimi (UI/UX)**  
✅ Arayüz için **JavaFX** veya **Spring Boot + React** gibi daha modern teknolojiler tercih edilebilir.  
✅ **Karanlık mod (Dark Mode)** desteği eklenebilir.  
✅ **Bildirim sistemi** eklenerek çalışanlara izin onay/red durumları anında bildirilebilir.  

### 4️⃣ **Yeni Özellikler**  
✅ **Raporları PDF/Excel formatında indirme** seçeneği eklenebilir.  
✅ **Mobil uyumlu bir web arayüzü** geliştirilebilir.  
✅ **Zamanlayıcı (Scheduler) ve hatırlatma sistemi** eklenerek izin, mesai, maaş günü gibi durumlar otomatik hatırlatılabilir.  

---

## 🎬 **Tanıtım Videosu**  

### 🎥 **YouTube**  

👀 Detaylı bir inceleme ve ilk izlenim için **YouTube** videoma [buradan](https://youtu.be/k2qjKHQSETk) erişebilirsiniz.

---

## 🤝 Katkıda Bulunma

1️⃣ **Forklayın**:

```
git clone https://github.com/semhtemiz/personnel_management_system.git
```

2️⃣ **Yeni Bir Branch Açın**:

```
git checkout -b feature/yeni-ozellik
```

3️⃣ **Kodunuzu Ekleyin ve Commit Yapın**:

```
git commit -am "Yeni özellik eklendi"
```

4️⃣ **Pull Request Açın ve Katkıda Bulunun!** 🚀

---

## 📜 Lisans

Bu proje, **MIT Lisansı** altında sunulmaktadır. Detaylar için `LICENSE` dosyasına bakabilirsiniz.

---



