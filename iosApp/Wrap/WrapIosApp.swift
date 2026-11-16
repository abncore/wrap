import SwiftUI
import WrapUi

/// The iOS app: starts the shared Kotlin code and shows the shared Compose screens.
@main
struct WrapIosApp: App {
    init() {
        MainViewControllerKt.startWrap()
    }

    var body: some Scene {
        WindowGroup {
            ComposeScreen()
                // Compose draws behind the status bar and home indicator and pads itself.
                .ignoresSafeArea()
        }
    }
}

private struct ComposeScreen: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
