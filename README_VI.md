# SoundCraft

<p align="left">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-green?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft Version" />
  <img src="https://img.shields.io/badge/Fabric-0.16.0-blue?style=for-the-badge" alt="Fabric Loader" />
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" alt="License" />
</p>

**SoundCraft** là một mod Fabric dành cho Minecraft giúp đồng bộ hóa nhạc thời gian thực trực tiếp vào game của bạn. Với sự hỗ trợ của một tiện ích mở rộng trình duyệt (extension) nhẹ đi kèm, mod có thể ghi nhận thông tin bài hát (tên bài, nghệ sĩ, ảnh bìa album, trạng thái phát nhạc) từ các nền tảng như **SoundCloud**, **Spotify**, và **YouTube**, sau đó hiển thị trên giao diện HUD đẹp mắt hoặc bảng điều khiển tương tác trong game. Bạn thậm chí có thể điều khiển trình phát nhạc bằng phím tắt hoặc giao diện GUI trong Minecraft!

---

## 🚀 Tính năng nổi bật

*   **Đồng bộ hóa thời gian thực**: Đồng bộ hóa tên bài hát, nghệ sĩ, URL ảnh bìa, trạng thái phát/tạm dừng và tiến trình phát nhạc (progress).
*   **Hỗ trợ đa nền tảng**: Tự động nhận diện và đồng bộ từ các trình phát web của **SoundCloud**, **Spotify** và **YouTube**.
*   **Giao diện HUD đẹp mắt**: Hiển thị HUD sắc nét trong game với tên bài hát, nghệ sĩ, ảnh bìa và thanh tiến trình phát nhạc tự động đổi màu theo màu chủ đạo của ảnh bìa!
*   **Bảng điều khiển tương tác**: Mở màn hình điều khiển trong game (phím mặc định: `J`) với phong cách kính mờ (glassmorphism) hiện đại, thanh tiến trình trực quan và các nút điều khiển nhạc (⏮ Trước, ⏯ Phát/Tạm dừng, ⏭ Kế tiếp).
*   **Phím tắt điều khiển trực tiếp**: Điều khiển nhạc trên trình duyệt mà không cần phải thoát Minecraft hoặc dùng tổ hợp phím ALT-TAB.
*   **Kết nối gọn nhẹ**: Kết nối các client cục bộ một cách an toàn thông qua một WebSocket Server cục bộ chạy trên cổng `8887`.

---

## 🎮 Hệ thống phím tắt

Sử dụng các phím tắt mặc định sau khi đang ở trong trò chơi (không mở giao diện chat/khác):

| Hành động | Phím mặc định | Mô tả |
| :--- | :--- | :--- |
| **Mở bảng điều khiển** | `J` | Mở giao diện điều khiển (Controller GUI) phong cách kính mờ. |
| **Phát / Tạm dừng** | `Numpad 5` (Phím số 5) | Bật/tắt phát nhạc trên trình duyệt. |
| **Bài tiếp theo** | `Numpad 6` (Phím số 6) | Chuyển sang bài tiếp theo. |
| **Bài trước đó** | `Numpad 4` (Phím số 4) | Quay lại bài trước đó. |

---

## 📥 Hướng dẫn cài đặt & Thiết lập

Lựa chọn một trong các phương thức cài đặt dưới đây:

### 📦 Tải bản cài đặt sẵn (Releases)

Tải xuống trực tiếp các file đã được biên dịch sẵn:

<p align="left">
  <a href="https://github.com/Huyphan68080/Mod-SoundCraft/releases/latest/download/soundcraft-1.0.0.jar">
    <img src="https://img.shields.io/badge/Tải%20về-Minecraft%20Mod%20(.jar)-green?style=for-the-badge&logo=minecraft&logoColor=white" alt="Tải về Minecraft Mod" />
  </a>
  &nbsp;&nbsp;
  <a href="https://github.com/Huyphan68080/Mod-SoundCraft/releases/latest/download/soundcraft-extension.zip">
    <img src="https://img.shields.io/badge/Tải%20về-Browser%20Extension%20(.zip)-blue?style=for-the-badge&logo=googlechrome&logoColor=white" alt="Tải về Browser Extension" />
  </a>
</p>

#### 1. Cài đặt Fabric Mod
1. Tải về file `.jar` của Minecraft Mod bằng nút tải phía trên.
2. Copy file `.jar` đó vào thư mục `.minecraft/mods/` của bạn.
3. Chạy game Minecraft với Fabric Loader 0.16.0+ phiên bản 1.21.1.

#### 2. Cài đặt Browser Extension
*   **Cách A (Nhanh nhất)**:
    1. Tải file Browser Extension `.zip` bằng nút tải phía trên và giải nén.
    2. Mở trình duyệt và truy cập `chrome://extensions/`.
    3. Bật **Chế độ dành cho nhà phát triển** (Developer mode) ở góc trên bên phải.
    4. Nhấn **Tải thư mục đã giải nén** (Load unpacked) và chọn thư mục vừa giải nén.
*   **Cách B (Từ mã nguồn đã clone)**:
    1. Mở trình duyệt và truy cập `chrome://extensions/`.
    2. Bật **Chế độ dành cho nhà phát triển** (Developer mode) ở góc trên bên phải.
    3. Nhấn **Tải thư mục đã giải nén** (Load unpacked) và chọn thư mục `extension` nằm bên trong thư mục dự án đã clone.

---

### 🛠️ Tự biên dịch từ mã nguồn (Dành cho Lập trình viên)

<details>
<summary>Nhấn để xem hướng dẫn biên dịch</summary>

#### Yêu cầu hệ thống
*   Java 17 trở lên
*   Gradle (được cung cấp sẵn qua Gradle wrapper)

#### Biên dịch
1. Clone mã nguồn dự án:
   ```bash
   git clone https://github.com/Huyphan68080/Mod-SoundCraft.git
   cd Mod-SoundCraft
   ```
2. Build dự án bằng Gradle:
   *   **Windows**: `.\gradlew build`
   *   **Linux/macOS**: `./gradlew build`
3. File `.jar` đã biên dịch sẽ nằm tại thư mục `build/libs/soundcraft-1.0.0.jar`. Hãy copy file này vào thư mục `.minecraft/mods/` của bạn.

</details>

---

## 🛠 Nguyên lý hoạt động (Kiến trúc)

```mermaid
graph TD
    Browser[Trình duyệt: SoundCloud / Spotify / YouTube] -->|Cập nhật trạng thái| ContentScript[Content Script]
    ContentScript -->|Gửi tin nhắn| ServiceWorker[Background Service Worker]
    ServiceWorker -->|WebSocket ws://127.0.0.1:8887| MCServer[Minecraft Client WebSocket Server]
    MCServer -->|Cập nhật giao diện| HUD[HUD trong game]
    MCServer -->|Cập nhật giao diện| GUI[Giao diện điều khiển GUI]
    
    GUI -->|Gửi lệnh điều khiển| MCServer
    MCServer -->|WebSocket ws://127.0.0.1:8887| ServiceWorker
    ServiceWorker -->|Gửi lệnh đến tab| ContentScript
    ContentScript -->|Kích hoạt nút trên web| Browser
```

---

## ⚙️ Cấu hình & Cổng mạng

*   Mod khởi chạy một WebSocket server nội bộ lắng nghe tại cổng **`8887`** (`ws://127.0.0.1:8887`).
*   Hãy đảm bảo không có ứng dụng nào khác sử dụng cổng `8887` trước khi mở game Minecraft.
*   Extension trình duyệt sẽ tự động thử kết nối lại với Minecraft nếu kết nối bị gián đoạn.

---

## 📄 Giấy phép

Dự án này được phát hành dưới giấy phép MIT License - xem file [LICENSE](LICENSE) để biết thêm chi tiết.
