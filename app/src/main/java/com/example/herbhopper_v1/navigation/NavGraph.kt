package com.example.herbhopper_v1.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.herbhopper_v1.data.Product
import com.example.herbhopper_v1.data.SessionManager
import com.example.herbhopper_v1.ui.admin.*
import com.example.herbhopper_v1.ui.patient.*
import com.example.herbhopper_v1.ui.seller.*
import com.example.herbhopper_v1.viewmodel.CartViewModel
import com.example.herbhopper_v1.viewmodel.OrderViewModel
import com.example.herbhopper_v1.viewmodel.PaymentMethod
import com.example.herbhopper_v1.viewmodel.ProductViewModel
import com.example.herbhopper_v1.viewmodel.ProfileViewModel
import com.example.herbhopper_v1.viewmodel.TransactionStatus

/** Pantalla inicial de cada rol. */
private fun homeRouteFor(role: String): String = when (role.uppercase()) {
    "ADMIN" -> "store_governance"
    "SELLER" -> "seller_dashboard"
    else -> "catalog"
}

@Composable
fun NavGraph(navController: NavHostController) {
    val cartViewModel: CartViewModel = viewModel()
    val productViewModel: ProductViewModel = viewModel()
    val orderViewModel: OrderViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    val cartItems by cartViewModel.items.collectAsState()

    // Datos del pedido en curso para ePayco y la pantalla de confirmación
    var lastPaymentStatus by remember { mutableStateOf<TransactionStatus>(TransactionStatus.Idle) }
    var lastOrderId by remember { mutableStateOf("") }
    var lastOrderAmount by remember { mutableStateOf(0.0) }
    var lastPaymentMethod by remember { mutableStateOf<PaymentMethod>(PaymentMethod.CreditCard) }

    /** Entra al inicio del rol y limpia la pila (no se puede volver al login con "atrás"). */
    fun goHome(role: String) {
        navController.navigate(homeRouteFor(role)) { popUpTo(0) { inclusive = true } }
    }

    /** Cierra sesión (revoca el token en el servidor) y vuelve al acceso. */
    fun logout() {
        profileViewModel.logout {
            cartViewModel.clearCart()
            navController.navigate("secure_access") { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(onTimeout = {
                // Sesión guardada (token cifrado): entra directo al inicio de su rol
                if (SessionManager.isLoggedIn() && SessionManager.getAccessToken() != null) {
                    goHome(SessionManager.getRole())
                } else {
                    navController.navigate("secure_access") { popUpTo("splash") { inclusive = true } }
                }
            })
        }

        // ─── Acceso ────────────────────────────────────────────────────────
        composable("secure_access") {
            SecureAccessScreen(
                onGoogleLogin = { navController.navigate("login") },
                onBiometricClick = { navController.navigate("login") },
                onPinClick = { navController.navigate("login") },
                onSignUpClick = { navController.navigate("create_account") },
                onForgotPasswordClick = { navController.navigate("forgot_password") },
                onAdminAccess = { navController.navigate("login") },
                onSellerAccess = { navController.navigate("login") }
            )
        }
        composable("forgot_password") {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable("login") {
            LoginScreen(
                onPatientSuccess = { goHome("PATIENT") },
                onSellerSuccess = { goHome("SELLER") },
                onAdminSuccess = { goHome("ADMIN") },
                onForgotPasswordClick = { navController.navigate("forgot_password") },
                onSignUpClick = { navController.navigate("create_account") },
                profileViewModel = profileViewModel
            )
        }
        composable("create_account") {
            CreateAccountScreen(
                onSignUpSuccess = { goHome("PATIENT") },
                onSellerSignUpSuccess = { goHome("SELLER") },
                onLoginClick = { navController.navigate("login") },
                profileViewModel = profileViewModel
            )
        }

        // ─── Paciente ──────────────────────────────────────────────────────
        composable("home") {
            MainMenuScreen(
                onLogout = { logout() },
                onCatalogClick = { navController.navigate("catalog") },
                onOrdersClick = { navController.navigate("order_history") },
                onPrescriptionClick = { navController.navigate("prescription_validation") },
                onProfileClick = { navController.navigate("profile") },
                onHelpClick = { navController.navigate("help_center") }
            )
        }
        composable("catalog") {
            CatalogScreen(
                productViewModel = productViewModel,
                onProductClick = { product ->
                    selectedProduct = product
                    navController.navigate("product_details")
                },
                onAddToCart = { cartViewModel.addToCart(it) },
                onMenuClick = { navController.navigate("home") },
                onCartClick = { navController.navigate("cart") },
                onHomeClick = { },
                onOrdersClick = { navController.navigate("order_history") },
                onScriptsClick = { navController.navigate("prescription_validation") },
                onProfileClick = { navController.navigate("profile") }
            )
        }
        composable("product_details") {
            selectedProduct?.let { product ->
                ProductDetailsScreen(
                    product = product,
                    onBack = { navController.popBackStack() },
                    onAddToCart = { cartViewModel.addToCart(it) },
                    cartItemCount = cartItems.size,
                    onCartClick = { navController.navigate("cart") }
                )
            }
        }
        composable("prescription_validation") {
            PrescriptionValidationScreen(
                onBack = { navController.popBackStack() },
                onHelpClick = { navController.navigate("help_center") }
            )
        }
        composable("cart") {
            CartScreen(
                viewModel = cartViewModel,
                onBack = { navController.popBackStack() },
                onCheckout = { navController.navigate("checkout") }
            )
        }
        composable("checkout") {
            // Referencia única del pedido: es también la factura (x_id_invoice) en ePayco
            val orderId = remember { "HH-${System.currentTimeMillis()}-${(100..999).random()}" }
            CheckoutScreen(
                cartItems = cartItems,
                orderId = orderId,
                onBack = { navController.popBackStack() },
                onOrderCreated = { order, method ->
                    lastOrderId = order.orderId
                    lastOrderAmount = order.totalAmount // total recalculado por el servidor
                    lastPaymentMethod = method
                    navController.navigate("epayco_webview")
                },
                onManageAddresses = { navController.navigate("profile_settings/${ProfileOptionDestination.ADDRESSES.key}") },
                onUploadPrescription = { navController.navigate("prescription_validation") }
            )
        }
        composable("epayco_webview") {
            EpaycoWebViewScreen(
                amount = lastOrderAmount,
                orderId = lastOrderId,
                description = "Pedido HerbHopper - $lastOrderId",
                paymentMethod = lastPaymentMethod,
                onBack = {
                    // El usuario abandonó el pago: se anula el pedido y se libera el stock reservado
                    orderViewModel.cancelMyOrder(lastOrderId)
                    navController.popBackStack("checkout", inclusive = true)
                },
                onFinished = { status ->
                    lastPaymentStatus = status
                    when (status) {
                        is TransactionStatus.Accepted, is TransactionStatus.Pending -> cartViewModel.clearCart()
                        is TransactionStatus.Rejected -> orderViewModel.cancelMyOrder(lastOrderId)
                        else -> Unit // Error de consulta: el resultado real llega al backend por la confirmación de ePayco
                    }
                    orderViewModel.refresh()
                    navController.navigate("payment_confirmation") {
                        popUpTo("checkout") { inclusive = true }
                    }
                }
            )
        }
        composable("payment_confirmation") {
            PaymentConfirmationScreen(
                status = lastPaymentStatus,
                orderId = lastOrderId,
                totalAmount = lastOrderAmount,
                onGoToOrders = {
                    navController.navigate("order_tracking/$lastOrderId") {
                        popUpTo("payment_confirmation") { inclusive = true }
                    }
                },
                onRetry = {
                    navController.navigate("cart") {
                        popUpTo("payment_confirmation") { inclusive = true }
                    }
                }
            )
        }
        composable("order_history") {
            OrderHistoryScreen(
                viewModel = orderViewModel,
                onHomeClick = { navController.navigate("catalog") },
                onOrdersClick = { },
                onScriptsClick = { navController.navigate("prescription_validation") },
                onProfileClick = { navController.navigate("profile") },
                onCartClick = { navController.navigate("cart") },
                onOrderClick = { orderId -> navController.navigate("order_tracking/$orderId") }
            )
        }
        composable(
            "order_tracking/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            OrderTrackingScreen(
                orderId = backStackEntry.arguments?.getString("orderId") ?: "",
                viewModel = orderViewModel,
                onBack = { navController.popBackStack() },
                onHomeClick = { navController.navigate("catalog") },
                onOrdersClick = { navController.navigate("order_history") },
                onScriptsClick = { navController.navigate("prescription_validation") },
                onProfileClick = { navController.navigate("profile") }
            )
        }
        composable("profile") {
            ProfileScreen(
                onHomeClick = { navController.navigate("catalog") },
                onOrdersClick = { navController.navigate("order_history") },
                onScriptsClick = { navController.navigate("prescription_validation") },
                onLogout = { logout() },
                onOptionClick = { option -> navController.navigate("profile_settings/$option") },
                viewModel = profileViewModel
            )
        }
        composable(
            "profile_settings/{option}",
            arguments = listOf(navArgument("option") { type = NavType.StringType })
        ) { backStackEntry ->
            val option = backStackEntry.arguments?.getString("option") ?: ""
            ProfileSettingsScreen(
                title = option,
                onBack = { navController.popBackStack() },
                onAccountDeleted = {
                    cartViewModel.clearCart()
                    navController.navigate("secure_access") { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable("help_center") {
            HelpCenterScreen(
                onBack = { navController.popBackStack() },
                onChatClick = { navController.navigate("chat") }
            )
        }
        composable("chat") {
            ChatScreen(onBack = { navController.popBackStack() })
        }

        // ─── Vendedor ──────────────────────────────────────────────────────
        composable("seller_dashboard") {
            SellerDashboardScreen(
                viewModel = productViewModel,
                orderViewModel = orderViewModel,
                onNavigate = { navController.navigate(it) }
            )
        }
        composable("inventory") {
            InventoryScreen(
                viewModel = productViewModel,
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
                onAddProduct = { navController.navigate("inventory_crud") },
                onEditProduct = { productId -> navController.navigate("inventory_crud/$productId") }
            )
        }
        composable("inventory_crud") {
            InventoryCrudScreen(
                viewModel = productViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "inventory_crud/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.IntType })
        ) { backStackEntry ->
            InventoryCrudScreen(
                productId = backStackEntry.arguments?.getInt("productId"),
                viewModel = productViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("incoming_orders") {
            IncomingOrdersScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
                viewModel = orderViewModel
            )
        }
        composable(
            "order_details/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            OrderDetailsScreen(
                orderId = backStackEntry.arguments?.getString("orderId") ?: "",
                onBack = { navController.popBackStack() },
                viewModel = orderViewModel
            )
        }
        composable("store_profile") {
            StoreProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
                onLogout = { logout() },
                onOptionClick = { option -> navController.navigate("profile_settings/$option") }
            )
        }

        // ─── Administrador ─────────────────────────────────────────────────
        composable("store_governance") {
            StoreGovernanceScreen(
                onNavigate = { navController.navigate(it) },
                onLogout = { logout() }
            )
        }
        composable("admin_profile") {
            AdminProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
                onLogout = { logout() }
            )
        }
        composable("user_management") {
            UserManagementScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) }
            )
        }
        composable("audit_panel") {
            AuditPanelScreen(onNavigate = { navController.navigate(it) })
        }
        composable("global_analytics") {
            GlobalAnalyticsScreen(onNavigate = { navController.navigate(it) })
        }
        composable("system_infrastructure") {
            SystemInfrastructureScreen(onNavigate = { navController.navigate(it) })
        }
        composable("support_inbox") {
            SupportInboxScreen(onBack = { navController.popBackStack() })
        }
    }
}
